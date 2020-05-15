package com.example.lieu;

import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.abs;

public class CellFragment extends Fragment implements View.OnClickListener, Swap {

    // The ID of the cell to be shown
    private int cellID = -1;

    // The index of the data being shown
    private int data_index = -1;

    // The cell identifier text view
    private TextView titleTextView;

    // The back button
    private Button backButton;

    // The next button
    private Button nextButton;

    // The access-point name text view
    private TextView selectionTextView;

    // Dummy selection
    private ArrayList<String> dummy_strings;

    // The bar chart
    private BarChart barChart;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Retreive the DataManager singleton
        DataManager dataManager = DataManager.getInstance();

        // Retreive the cell ID from the bundle
        this.cellID = getArguments().getInt("cellID", -1) - 1;


        return inflater.inflate(R.layout.fragment_cell, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        String selectionText = "No data!";

        // Initialize the cell title text view
        this.titleTextView = getView().findViewById(R.id.cell_text_view);

        // Set the text
        this.titleTextView.setText("Cell " + (this.cellID + 1));

        // Initialize and bind the buttons
        this.backButton = getView().findViewById(R.id.back_button);
        this.nextButton = getView().findViewById(R.id.next_button);

        this.backButton.setOnClickListener(this);
        this.nextButton.setOnClickListener(this);

        // Initialize the selection text view
        this.selectionTextView = getView().findViewById(R.id.selection_text_view);

        // Bind the bar chart
        this.barChart = getView().findViewById(R.id.bar_chart);

        // Extract cell data
        ArrayList<AccessPointResult> data = getCell().getAccessPointResults();

        System.out.println("This cell has " + data.size() + " entries!");


        // Set the initial text
        if (data.size() != 0) {
            data_index = 0;
            selectionText = data.get(0).getBssid();
            this.backButton.setEnabled(false);
        } else {
            this.backButton.setEnabled(false);
            this.nextButton.setEnabled(false);
        }

        this.selectionTextView.setText(selectionText);

        // Refresh the line chart
        refreshBarChart();

    }

    @Override
    public void onClick(View v) {

        // Update the index
        switch (v.getId()) {
            case R.id.back_button:
                if (data_index > 0) {
                    nextButton.setEnabled(true);
                    data_index--;
                }
                if (data_index == 0) {
                    backButton.setEnabled(false);
                }
                break;
            case R.id.next_button:
                if (data_index < getCell().getAccessPointResults().size() - 1) {
                    backButton.setEnabled(true);
                    data_index++;
                }
                if (data_index == getCell().getAccessPointResults().size() - 1) {
                    nextButton.setEnabled(false);
                }
                break;
        }

        // Update the displayed data
        AccessPointResult result = getCell().getAccessPointResults().get(data_index);
        this.selectionTextView.setText(result.getBssid());
        this.refreshBarChart();
    }

    // Returns the cell to which this fragment refers to
    public Cell getCell () {
        return DataManager.getInstance().getCells().get(this.cellID);
    }


    // Creates a probability histogram
    private BarDataSet getBarDataFromSamples(ArrayList<Double> samples) {

        ArrayList<BarEntry> entries = new ArrayList<>();
        double n = (double)samples.size();

        // Create a histogram of probabilities
        for (int i = 0; i < 100; ++i) {
            double y = 0.0f;
            double x = (double)i;

            // Find how many occurrences of this RSS value occur in the sample
            for (Double d : samples) {
                if (abs(d) == x) {
                    y = y + 1.0f;
                }
            }

            // Add entry
            entries.add(new BarEntry((float)x,(float)(y / n)));
        }

        BarDataSet set = new BarDataSet(entries, "RSS");
        set.setColor(Color.rgb(155, 155, 155));
        set.setValueTextColor(Color.rgb(155,155,155));

        return set;
    }

    // Configures the chart
    private void refreshBarChart () {

        // Check the data index
        if (data_index < 0) {
            return;
        }

        // Grab the cell data
        System.out.println("Refreshing for data_index: " + data_index);
        AccessPointResult ap = getCell().getAccessPointResults().get(data_index);

        // Setup the X axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(true);

        // Setup the X axis data
        ArrayList<String> xvalues = new ArrayList<String>();
        for (int i = 0; i < 100; ++i) {
            xvalues.add("" + i);
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xvalues));

        // Setup the Y axis
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0.0f);
        yAxis.setAxisMaximum(1.0f);
        barChart.getAxisRight().setEnabled(false);

        // Setup Y axis data
        BarDataSet dataSet = getBarDataFromSamples(ap.getSamples());
        BarData data = new BarData(dataSet);
        // data.setBarWidth(1f);

        this.barChart.setTouchEnabled(false);
        this.barChart.setData(data);
        this.barChart.setFitBars(true);
        this.barChart.invalidate();

    }

    public void swappingOut () {

    }
}
