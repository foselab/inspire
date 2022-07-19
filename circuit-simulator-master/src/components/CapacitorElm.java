package components;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.StringTokenizer;

import utils.EditInfo;

public class CapacitorElm extends CircuitElm {
	private double capacitance;
	double compResistance, voltdiff;
	Point plate1[], plate2[];
	public static final int FLAG_BACK_EULER = 2;

	public CapacitorElm(int xx, int yy) {
		super(xx, yy);
		setCapacitance(1e-5);
	}

	public CapacitorElm(int xa, int ya, int xb, int yb, int f, double cap, double voltDiff) {
		super(xa, ya, xb, yb, f);
		setCapacitance(cap);
		this.voltdiff = voltDiff;
	}

	public CapacitorElm(int xa, int ya, int xb, int yb, int f, StringTokenizer st) {
		this(xa, ya, xb, yb, f, new Double(st.nextToken()).doubleValue(), new Double(st.nextToken()).doubleValue());
	}

	boolean isTrapezoidal() {
		return (flags & FLAG_BACK_EULER) == 0;
	}

	@Override
	public void setNodeVoltage(int n, double c) {
		super.setNodeVoltage(n, c);
		voltdiff = volts[0] - volts[1];
	}

	@Override
	public void reset() {
		current = curcount = 0;
		// put small charge on caps when reset to start oscillators
		voltdiff = 1e-3;
	}

	@Override
	public int getDumpType() {
		return 'c';
	}

	@Override
	public String dump() {
		return super.dump() + " " + getCapacitance() + " " + voltdiff;
	}

	@Override
	public void setPoints() {
		super.setPoints();
		double f = (dn / 2 - 4) / dn;
		// calc leads
		lead1 = interpPoint(point1, point2, f);
		lead2 = interpPoint(point1, point2, 1 - f);
		// calc plates
		plate1 = newPointArray(2);
		plate2 = newPointArray(2);
		interpPoint2(point1, point2, plate1[0], plate1[1], f, 12);
		interpPoint2(point1, point2, plate2[0], plate2[1], 1 - f, 12);
	}

	@Override
	public void draw(Graphics g) {
		int hs = 12;
		setBbox(point1, point2, hs);

		// draw first lead and plate
		setVoltageColor(g, volts[0]);
		drawThickLine(g, point1, lead1);
		setPowerColor(g, false);
		drawThickLine(g, plate1[0], plate1[1]);
		if (sim.getPowerCheckItem().getState())
			g.setColor(Color.gray);

		// draw second lead and plate
		setVoltageColor(g, volts[1]);
		drawThickLine(g, point2, lead2);
		setPowerColor(g, false);
		drawThickLine(g, plate2[0], plate2[1]);

		updateDotCount();
		if (sim.getDragElm() != this) {
			drawDots(g, point1, lead1, curcount);
			drawDots(g, point2, lead2, -curcount);
		}
		drawPosts(g);
		if (sim.getShowValuesCheckItem().getState()) {
			String s = getShortUnitText(getCapacitance(), "F");
			drawValues(g, s, hs);
		}
	}

	@Override
	public void stamp() {
		// capacitor companion model using trapezoidal approximation
		// (Norton equivalent) consists of a current source in
		// parallel with a resistor. Trapezoidal is more accurate
		// than backward euler but can cause oscillatory behavior
		// if RC is small relative to the timestep.
		if (isTrapezoidal())
			compResistance = sim.getTimeStep() / (2 * getCapacitance());
		else
			compResistance = sim.getTimeStep() / getCapacitance();
		sim.stampResistor(nodes[0], nodes[1], compResistance);
		sim.stampRightSide(nodes[0]);
		sim.stampRightSide(nodes[1]);
	}

	@Override
	public void startIteration() {
		if (isTrapezoidal())
			curSourceValue = -voltdiff / compResistance - current;
		else
			curSourceValue = -voltdiff / compResistance;

		System.out.println("CapacitorElm: compResistance = " + compResistance + ", curSourceValue = " + curSourceValue
				+ ", current = " + current + ", voltdiff =" + voltdiff);
	}

	@Override
	void calculateCurrent() {
		System.out.println(this.getClass().getSimpleName() + " - volts[0] = " + volts[0] + ", volts[1] = " + volts[1]);
		double voltdiff = volts[0] - volts[1];
		/*
		 * we check compResistance because this might get called before stamp(), which
		 * sets compResistance, causing infinite current
		 */
		if (compResistance > 0)
			current = voltdiff / compResistance + curSourceValue;

		System.out.println(this.getClass().getSimpleName() + " - current set to " + current);
	}

	double curSourceValue;

	@Override
	public void doStep() {
		System.out.println("CapacitorElm: nodes[0] = " + nodes[0] + ", nodes[1] = " + nodes[1] + ", curSourceValue = "
				+ curSourceValue);
		sim.stampCurrentSource(nodes[0], nodes[1], curSourceValue);
	}

	@Override
	public void getInfo(String arr[]) {
		arr[0] = "capacitor";
		getBasicInfo(arr);
		arr[3] = "C = " + getUnitText(getCapacitance(), "F");
		arr[4] = "P = " + getUnitText(getPower(), "W");
		// double v = getVoltageDiff();
		// arr[4] = "U = " + getUnitText(.5*capacitance*v*v, "J");
	}

	@Override
	public EditInfo getEditInfo(int n) {
		if (n == 0)
			return new EditInfo("Capacitance (F)", getCapacitance(), 0, 0);
		if (n == 1) {
			EditInfo ei = new EditInfo("", 0, -1, -1);
			ei.setCheckbox(new Checkbox("Trapezoidal Approximation", isTrapezoidal()));
			return ei;
		}
		return null;
	}

	@Override
	public void setEditValue(int n, EditInfo ei) {
		if (n == 0 && ei.getValue() > 0)
			setCapacitance(ei.getValue());
		if (n == 1) {
			if (ei.getCheckbox().getState())
				flags &= ~FLAG_BACK_EULER;
			else
				flags |= FLAG_BACK_EULER;
		}
	}

	@Override
	public int getShortcut() {
		return 'c';
	}

	public double getCapacitance() {
		return capacitance;
	}

	public void setCapacitance(double capacitance) {
		this.capacitance = capacitance;
	}
}