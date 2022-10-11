package lungsimulator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import org.json.simple.parser.ParseException;
import lungsimulator.components.Archetype;
import lungsimulator.components.Patient;
import lungsimulator.utils.YamlReader;
import simulator.CirSim;

@SuppressWarnings("ucd")
public class LungSimulator {
	// Define the new plotter
	RealTimePlot rtp = new RealTimePlot();

	public Patient patient;
	public Archetype archetype;
	public GraphicInterface userInterface = new GraphicInterface();

	/**
	 * Init the lung simulator by reading and validating the patient model and
	 * archetype and set the frame configuration
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 * @throws Exception
	 */
	public LungSimulator() throws FileNotFoundException, IOException, ParseException, Exception {
		// Read patient model and patient archetype
		YamlReader yamlReader = new YamlReader();
		patient = yamlReader.readPatientModel();
		archetype = yamlReader.readArchetypeParameters();

		// Validation
		yamlReader.validator(patient, archetype);

		// Frame configuration
		userInterface.frameConfig(patient, archetype);
	}

	public void simulateCircuit() throws InterruptedException {
		// Create the circuit equivalent to the lung
		CirSim myCircSim = rtp.buildCircuitSimulator(patient, archetype);
		myCircSim.setTimeStep(0.1);

		double time = 0;

		while (true) {
			if (userInterface.getStateOfExecution()) {
				time += 0.1;

				/*
				 * if(time%2==0) { archetype.getParameters().put("TIME", String.valueOf(time));
				 * myCircSim = rtp.updateCircuitSimulator(patient, archetype); }
				 */

				// Analyze the circuit and simulate a step
				myCircSim.analyzeCircuit();
				myCircSim.loopAndContinue(true);

				Thread.sleep(100);

				userInterface.updateShownDataValues(time, myCircSim);
			}else {
				Thread.sleep(100);
			}
		}

	}

	/**
	 * Launch the application.
	 * 
	 * @throws ParseException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void main(String[] args)
			throws FileNotFoundException, IOException, ParseException, InterruptedException, Exception {

		LungSimulator mySimulator = new LungSimulator();
		mySimulator.simulateCircuit();
	}
}
