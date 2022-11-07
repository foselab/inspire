package org.vaadin.example;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import lungsimulator.LungSimulator;

public class RightVerticalLayout extends Composite<VerticalLayout> implements HasComponents{
	private Plot flowChart;
	private Plot pressureChart;
	
	private LungSimulator lungSimulator;
	
	public RightVerticalLayout(LungSimulator lungSimulator) {
		this.lungSimulator = lungSimulator;
		getContent().setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
		
		Div plotWrapper = new Div();
		add(plotWrapper);
		getContent().setFlexGrow(1, plotWrapper);
		
		this.flowChart = new Plot(lungSimulator.getCircuitBuilder().getFlowIds(), lungSimulator.getCircuitBuilder().getTimeline(), lungSimulator.getCircuitBuilder().getInitdataFlow(), "Flow");
		plotWrapper.add(flowChart);
		this.pressureChart = new Plot(lungSimulator.getCircuitBuilder().getPressureIds(), lungSimulator.getCircuitBuilder().getTimeline(), lungSimulator.getCircuitBuilder().getInitdataPressure(), "Pressure");
		plotWrapper.add(pressureChart);
	}

	public void updateChart(LungSimulator lungSimulator) {
		flowChart.updateFlowChart(lungSimulator.getCircuitBuilder().getTimeline(), lungSimulator.getCircuitBuilder().getInitdataFlow());
	}
}
