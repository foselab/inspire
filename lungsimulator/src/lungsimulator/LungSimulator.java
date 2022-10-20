package lungsimulator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import lungsimulator.components.Archetype;
import lungsimulator.components.Patient;
import lungsimulator.components.SimulatorParams;
import lungsimulator.graphic.GraphicInterface;
import lungsimulator.utils.Validator;
import lungsimulator.utils.YamlReader;
import simulator.CirSim;

/**
 * It contains the main logic for the lung simulator
 */
public class LungSimulator {
	/**
	 * Reference used to call methods for circuit construction
	 */
	private final transient CircuitBuilder circuitBuilder = new CircuitBuilder();

	/**
	 * Patient model description
	 */
	public transient Patient patient;

	/**
	 * Patient health description
	 */
	public transient Archetype archetype;

	/**
	 * Patient demographic data
	 */
	public transient SimulatorParams demographicData;

	/**
	 * Manages user interface properties
	 */
	public transient GraphicInterface userInterface = new GraphicInterface();

	/**
	 * Init the lung simulator by reading and validating the patient model and
	 * archetype and set the frame configuration
	 * 
	 * @throws FileNotFoundException one between the lung model and the archetype
	 *                               file (or both) is not located in config folder
	 * @throws IOException           the structure of YAML file is not correct: it
	 *                               could be either lung model or archetype (even
	 *                               both)
	 */
	public LungSimulator() throws FileNotFoundException, IOException {
		final String chosenSchema = userInterface.selectSchema();
		YamlReader yamlReader;

		if (chosenSchema != null) {
			if (chosenSchema.contains(",")) {
				final String[] fileNames = chosenSchema.split(",");
				yamlReader = new YamlReader(fileNames[0], fileNames[1], fileNames[2]);
			} else {
				yamlReader = new YamlReader(chosenSchema);
			}
			// Read patient model and patient archetype
			patient = yamlReader.readPatientModel();
			archetype = yamlReader.readArchetypeParameters();
			demographicData = yamlReader.readDemographicData();

			// Validation
			final Validator validator = new Validator();
			validator.evaluate(patient, archetype, demographicData);

			// Frame configuration
			userInterface.frameConfig(patient, archetype, demographicData);
		}
	}

	/**
	 * Circuit construction and resolution for each iteration
	 * 
	 * @throws InterruptedException
	 */
	public void simulateCircuit() throws InterruptedException {
		// Create the circuit equivalent to the lung
		final CirSim myCircSim = circuitBuilder.buildCircuitSimulator(patient, archetype);

		// TODO inserire circuitcomponents
		// userInterface.initValues(myCircSim)

		// ZMQ settings
		final ZContext context = new ZContext();
		final Socket socket = context.createSocket(SocketType.REQ);
		socket.connect("tcp://localhost:5555");

		final String message = "getPressure";
		double ventilatorValue;
		String replyMessage;

		// in secondi dall'inzio della simulazione
		double time = 0;
		long init_millisec = System.currentTimeMillis();

		final long delta_ms = (long) (0.02 * 1000); // timestep in millisec

		TimerTask repeatedTask = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("time " + (System.currentTimeMillis() - init_millisec));
				myCircSim.analyzeCircuit();
				myCircSim.loopAndContinue(false);

			}
		};

		long delay = 0;
		long period = (long) (delta_ms);

		while (userInterface.isWindowOpen()) {
			if (userInterface.getStateOfExecution()) {
				// Update ventilator value
				socket.send(message.getBytes(), 0);
				final byte[] reply = socket.recv(0);
				if (reply != null) {
					replyMessage = new String(reply, ZMQ.CHARSET);
					ventilatorValue = Double.parseDouble(replyMessage);
					circuitBuilder.updateVentilatorValue(ventilatorValue);
				}

				// update values for time dependent components
				if (circuitBuilder.isTimeDependentCir()) {
					circuitBuilder.updateCircuitSimulator(archetype, (System.currentTimeMillis() - init_millisec) / 1000.0);
				}

				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
				executor.scheduleAtFixedRate(repeatedTask, delay, period, TimeUnit.MILLISECONDS);
				Thread.sleep(100);
				executor.shutdown();

				userInterface.updateShownDataValues((System.currentTimeMillis() - init_millisec) / 1000.0, myCircSim);

				/*
				 * while (System.currentTimeMillis() - init_millisec < counter) {
				 * 
				 * // stop for delta millisecs while (System.currentTimeMillis() - last_millisec
				 * < delta_ms) ;
				 * 
				 * // Analyze the circuit and simulate a step myCircSim.analyzeCircuit();
				 * myCircSim.loopAndContinue(true);
				 * 
				 * last_millisec = System.currentTimeMillis(); // }
				 * 
				 * if (System.currentTimeMillis() - init_millisec > counter) {
				 * userInterface.updateShownDataValues((System.currentTimeMillis() -
				 * init_millisec) / 1000.0, myCircSim); counter += 500; }
				 */

			} else {
				Thread.sleep(100);
			}
		}
		socket.close();
		context.close();
	}

	/**
	 * Launch the application.
	 * 
	 * @throws IOException           the structure of YAML file is not correct: it
	 *                               could be either lung model or archetype (even
	 *                               both)
	 * @throws FileNotFoundException one between the lung model and the archetype
	 *                               file (or both) is not located in config folder
	 * @throws InterruptedException  internal error
	 */
	public static void main(final String[] args) throws FileNotFoundException, IOException, InterruptedException {

		final LungSimulator mySimulator = new LungSimulator();
		mySimulator.simulateCircuit();

	}
}
