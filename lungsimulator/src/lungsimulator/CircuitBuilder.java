package lungsimulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.udojava.evalex.Expression;

import components.ACVoltageElm;
import components.CapacitorElm;
import components.CircuitElm;
import components.DCVoltageElm;
import components.ExternalVoltageElm;
import components.ResistorElm;
import lungsimulator.components.Archetype;
import lungsimulator.components.Element;
import lungsimulator.components.Formula;
import lungsimulator.components.Patient;
import simulator.CirSim;

/**
 * Converts elements in patient model to their equivalent circuit elements
 */
public class CircuitBuilder {

	/**
	 * True if the model has at least one time dependent component
	 */
	private boolean timeDependentCir;
	
	/**
	 * The circuit
	 */
	private final transient CirSim cirSim;
	
	/**
	 * Contains every time dependent component and its formula
	 */
	private final transient Map<String, Formula> timeDependentElm;
	
	/**
	 * The elements of the circuit
	 */
	private final transient List<CircuitElm> elements;

	/**
	 * Init class fields
	 */
	public CircuitBuilder() {
		timeDependentCir = false;
		cirSim = new CirSim();
		timeDependentElm = new HashMap<>();
		elements = new ArrayList<>();
	}

	/**
	 * The method builds a circuit according to the patient's components and
	 * archetype
	 * @param patient patient model
	 * @param archetype archetype parameters
	 * @return the circuit
	 */
	public CirSim buildCircuitSimulator(final Patient patient, final Archetype archetype) {
		cirSim.setTimeStep(0.1);
		ResistorElm resistance;
		CapacitorElm capacitance;
		ACVoltageElm acVoltage;
		DCVoltageElm dcVoltage;
		ExternalVoltageElm externalVoltage;

		for (final Element element : patient.getElementsList()) {
			final String value = resolveFormula(element.getAssociatedFormula(), archetype.getParameters(), "0");

			if (!value.isEmpty()) {
				// resistance
				if ("ResistorElm".equals(element.getType())) {
					resistance = new ResistorElm(1, 1);
					resistance.setResistance(Double.parseDouble(value));
					circuitElmSetUp(element, resistance);
				}

				// capacitor
				if ("CapacitorElm".equals(element.getType())) {
					capacitance = new CapacitorElm(0, 0);
					capacitance.setCapacitance(Double.parseDouble(value));
					circuitElmSetUp(element, capacitance);
				}

				// acVoltage
				if ("ACVoltageElm".equals(element.getType())) {
					acVoltage = new ACVoltageElm(1, 1);
					acVoltage.setMaxVoltage(Double.parseDouble(value));
					circuitElmSetUp(element, acVoltage);
				}

				// dcVoltage
				if ("DCVoltageElm".equals(element.getType())) {
					dcVoltage = new DCVoltageElm(1, 1);
					dcVoltage.setMaxVoltage(Double.parseDouble(value));
					circuitElmSetUp(element, dcVoltage);
				}
			}

			// externalVoltage doesn't have a formula
			if ("ExternalVoltageElm".equals(element.getType())) {
				externalVoltage = new ExternalVoltageElm(1, 1, 28);
				circuitElmSetUp(element, externalVoltage);
			}
		}

		for (final CircuitElm circuitElm : elements) {
			circuitElm.setPoints();
		}

		cirSim.setElmList(elements);
		CircuitElm.sim = cirSim;

		return cirSim;
	}

	private void circuitElmSetUp(final Element element, final CircuitElm circuitElm) {
		circuitElm.setId(element.getElementName());
		circuitElm.setX(element.getX());
		circuitElm.setY(element.getY());
		circuitElm.setX2Y2(element.getX1(), element.getY1());

		if (element.isShowLeft()) {
			circuitElm.setIdLeft(element.getIdLeft());
		}

		if (element.isShowRight()) {
			circuitElm.setIdRight(element.getIdRight());
		}

		if (element.getAssociatedFormula().getIsTimeDependent()) {
			timeDependentElm.put(element.getElementName(), element.getAssociatedFormula());
		}
		elements.add(circuitElm);
	}

	/**
	 * Calculate element value
	 * 
	 * @param elementFormula formula description
	 * @param parameters     known values
	 * @param time           time value (it will be assigned only if there is TIME)
	 * @return element value
	 */
	private String resolveFormula(final Formula elementFormula, final Map<String, String> parameters,
			final String time) {
		String value = "";
		// if element has a formula
		if (elementFormula.getFormula() != null) {
			Expression formula = new Expression(elementFormula.getFormula());
			// assign to each variable its value
			for (final String var : elementFormula.getVariables()) {
				if (!"TIME".equals(var)) {
					formula = formula.setVariable(var, parameters.get(var));
				} else {
					formula = formula.setVariable(var, time);
					timeDependentCir = true;
				}
			}
			// resolve
			value = formula.eval().toString();
		}

		return value;
	}

	/**
	 * Update components values 
	 * @param archetype chosen archetype
	 * @param time new time for variable TIME
	 * @return the updated circuit
	 */
	public CirSim updateCircuitSimulator(final Archetype archetype, final double time) {

		for (final CircuitElm circuitElement : cirSim.getElmList()) {
			if (timeDependentElm.containsKey(circuitElement.getId())) {
				String value = resolveFormula(timeDependentElm.get(circuitElement.getId()), archetype.getParameters(),
						String.valueOf(time));

				// resistance
				if (circuitElement instanceof ResistorElm) {
					final ResistorElm resistance = (ResistorElm) circuitElement;
					resistance.setResistance(Double.parseDouble(value));
				}

				// capacitor
				if (circuitElement instanceof CapacitorElm) {
					final CapacitorElm capacitance = (CapacitorElm) circuitElement;
					capacitance.setCapacitance(Double.parseDouble(value));
				}

				// acVoltage
				if (circuitElement instanceof ACVoltageElm) {
					final ACVoltageElm acVoltage = (ACVoltageElm) circuitElement;
					acVoltage.setMaxVoltage(Double.parseDouble(value));
				}

				// dcVoltage
				if (circuitElement instanceof DCVoltageElm) {
					final DCVoltageElm dcVoltage = (DCVoltageElm) circuitElement;
					dcVoltage.setMaxVoltage(Double.parseDouble(value));
				}
			}
		}
		return cirSim;
	}

	public boolean isTimeDependentCir() {
		return timeDependentCir;
	}

	public void setTimeDependentCir(final boolean hasTimeDependency) {
		this.timeDependentCir = hasTimeDependency;
	}

}