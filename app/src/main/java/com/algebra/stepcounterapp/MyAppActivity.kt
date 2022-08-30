package com.algebra.stepcounterapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.android.synthetic.main.activity_my_app.*

import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MyAppActivity : AppCompatActivity( ) {

    private val TAG                                 = "MyAppActivity"
    private val chartValues                 = mutableListOf< BarEntry >( )
    val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
    val fitnessOptions                      = FitnessOptions.builder( )
        .addDataType( DataType.TYPE_STEP_COUNT_DELTA,      FitnessOptions.ACCESS_READ )
        .addDataType( DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ )
        .build( )

    override fun onCreate( savedInstanceState : Bundle? ) {
        super.onCreate( savedInstanceState )
        setContentView( R.layout.activity_my_app )

        initGraph( )

        if ( GoogleSignIn.hasPermissions( getGoogleAccount( ), fitnessOptions ) ) {
            subscribe( )
            refresh( )
        } else {
            Log.i( TAG, "Nemam dozvole" )
            GoogleSignIn.requestPermissions( this, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, getGoogleAccount( ), fitnessOptions )
        }

        bSave.setOnClickListener { refresh( ) }
        bCheck.setOnClickListener { refresh( ) }

    }

    private fun getGoogleAccount( ) = GoogleSignIn.getAccountForExtension( this, fitnessOptions )

    private fun subscribe( ) {
        Fitness.getRecordingClient( this, getGoogleAccount( ) )
            .subscribe( DataType.TYPE_STEP_COUNT_DELTA )
            .addOnSuccessListener { Log.i( TAG, "Successfully subscribed!" ) }
            .addOnFailureListener { e -> Log.w( TAG, "There was a problem subscribing.", e ) }
    }

    private fun refresh( ) {
        var numDays : Int? = when {
            rbToday.isChecked -> 1
            rbLastWeek.isChecked -> 7
            rbLastMonth.isChecked -> 30
            else -> {
                Toast
                    .makeText( this, "Morate odabrati vremenski period...", Toast.LENGTH_SHORT )
                    .show( )
               null
            }
        }
        val desiredNumSteps = getDesiredNumSteps( )

        if( desiredNumSteps==null || numDays==null )
            return

        val cal = Calendar.getInstance( )
        val endTime = cal.time.time / 1000

        cal.add( Calendar.DATE,        -( numDays-1 ) )
        cal.set( Calendar.HOUR_OF_DAY, 0 )
        cal.set( Calendar.MINUTE,      0 )
        cal.set( Calendar.SECOND,      0 )
        cal.set( Calendar.MILLISECOND, 0 )
        val startTime = cal.time.time / 1000

        val request = DataReadRequest.Builder( )
            .aggregate( DataType.AGGREGATE_STEP_COUNT_DELTA )
            .bucketByTime( numDays, TimeUnit.DAYS )
            .setTimeRange( startTime, endTime, TimeUnit.SECONDS )
            .build( )

        Fitness.getHistoryClient( this, GoogleSignIn.getAccountForExtension( this, fitnessOptions ) )
            .readData( request )
            .addOnSuccessListener { response ->
                val totalSteps = response.buckets
                    .flatMap { it.dataSets }
                    .flatMap { it.dataPoints }
                    .sumBy { it.getValue( Field.FIELD_STEPS ).asInt( ) }
                updateGraph( desiredNumSteps.toFloat( ), totalSteps.toFloat( ) )
            }
    }


    fun vrijeme( x : Long ) = SimpleDateFormat( "dd.MM.yyyy. HH:mm:ss" ).format( Date( x ) )


    private fun getDesiredNumSteps( ) : Int? {
        try {
            return etDesiredNumber.text.toString( ).toInt( )
        } catch ( e : Exception ) {
            Toast
                .makeText( this, "Morate unijeti željeni broj koraka...", Toast.LENGTH_SHORT )
                .show( )
        }
        return null
    }

    private fun initGraph( ) {
        chart.description.isEnabled = false
        chart.setMaxVisibleValueCount(10)
        chart.setPinchZoom(false)

        chart.setDrawBarShadow( true )
        chart.setDrawGridBackground( true )

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines( false )
        xAxis.setDrawLabels( false )

        chart.axisLeft.setDrawGridLines(false)
        chart.animateY( 1500 )

        chart.legend.isEnabled = true

        chartValues.add( BarEntry( 0f, 0f ) )
        chartValues.add( BarEntry( 1f, 0f ) )

        val dataSet = BarDataSet( chartValues, "Vrijednosti" )
        dataSet.colors = ColorTemplate.VORDIPLOM_COLORS.asList()
        dataSet.values = chartValues
        dataSet.label
        dataSet.setDrawValues( false )

        val entries = mutableListOf< LegendEntry >( )
        var entry = LegendEntry()
        entry.formColor = ColorTemplate.VORDIPLOM_COLORS[0]
        entry.label = "Desired"
        entries.add( entry )
        entry = LegendEntry( )
        entry.formColor = ColorTemplate.VORDIPLOM_COLORS[1]
        entry.label = "Actual"
        entries.add( entry )

        chart.legend.setCustom( entries )
        val dataSets = mutableListOf<BarDataSet>()
        dataSets.add(dataSet)

        val data = BarData(dataSets as List<IBarDataSet>?)
        chart.data = data
        chart.setFitBars( true )
    }

    fun updateGraph( f1 : Float, f2 : Float ) {
        chartValues[0] = BarEntry( 0f, f1 )
        chartValues[1] = BarEntry( 1f, f2 )
        val dataSet = chart.data.getDataSetByIndex(0) as BarDataSet
        dataSet.values = chartValues
        chart.animateY( 1500 )
        chart.data.notifyDataChanged( )
        chart.notifyDataSetChanged( )
        chart.invalidate( )
    }

}