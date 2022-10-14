package simulator;
// CirSim.java (c) 2010 by Paul Falstad

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.lang3.builder.ToStringBuilder;

import components.CapacitorElm;
import components.CircuitElm;
import components.CircuitNode;
import components.CircuitNodeLink;
import components.CurrentElm;
import components.GroundElm;
import components.InductorElm;
import components.RailElm;
import components.SwitchElm;
import components.VoltageElm;
import components.WireElm;
import scope.Scope;
import utils.ImportExportDialog;
import utils.RowInfo;

public class CirSim {

	private Random random;

	private int mouseMode = MODE_SELECT;
	public static final int MODE_DRAG_ROW = 2;
	public static final int MODE_DRAG_COLUMN = 3;
	public static final int MODE_SELECT = 6;
	public static final int infoWidth = 120;
	private int gridSize;

	int gridMask;

	int gridRound;
	boolean dragging;
	private boolean analyzeFlag;
	boolean dumpMatrix;
	private boolean useBufferedImage;
	boolean isMac;
	String ctrlMetaKey;
	private double t;
	private int scopeSelected = -1;
	int menuScope = -1;
	int hintType = -1, hintItem1, hintItem2;
	String stopMessage;
	private double timeStep;
	static final int HINT_LC = 1;
	static final int HINT_RC = 2;
	static final int HINT_3DB_C = 3;
	static final int HINT_TWINT = 4;
	static final int HINT_3DB_L = 5;
	/**
	 * List of circuit components
	 */
	private List<CircuitElm> elmList;
//    Vector setupList;
	private CircuitElm dragElm;

	CircuitElm menuElm;

	private CircuitElm mouseElm;

	CircuitElm stopElm;
	boolean didSwitch = false;
	int mousePost = -1;
	private CircuitElm plotXElm;

	private CircuitElm plotYElm;
	int draggingPost;
	SwitchElm heldSwitchElm;
	double circuitMatrix[][], circuitRightSide[], origRightSide[], origMatrix[][];
	RowInfo circuitRowInfo[];
	int circuitPermute[];
	boolean circuitNonLinear;
	int voltageSourceCount;
	int circuitMatrixSize, circuitMatrixFullSize;
	boolean circuitNeedsMap;
	public boolean useFrame;
	int scopeCount;
	Scope scopes[];
	private static ImportExportDialog impDialog;

	private static String muString = "u";
	private static String ohmString = "ohm";
	String clipboard;

	// TODO
	public int ventilatorIndex;

	public int getrand(int x) {
		int q = random.nextInt();
		if (q < 0)
			q = -q;
		return q % x;
	}

	int steps = 0;

	/**
	 * How circuit components are interconnected
	 */
	private Vector<CircuitNode> nodeList;

	CircuitElm voltageSources[];

	/**
	 * Returns the nth node of nodeList
	 */
	public CircuitNode getCircuitNode(int n) {
		if (n >= getNodeList().size())
			return null;
		return getNodeList().elementAt(n);
	}

	public CircuitElm getElm(int n) {
		if (n >= getElmList().size())
			return null;
		return getElmList().get(n);
	}

	public void analyzeCircuit() {
		//If there are no elements, return
		if (elmList.isEmpty())
			return;
		stopMessage = null;
		stopElm = null;
		int i, j;
		int vscount = 0;
		// System.out.println("nodeList: " + this.nodeList);
		setNodeList(new Vector<CircuitNode>());
		// System.out.println("nodeList: " + this.nodeList);
		boolean gotGround = false;
		boolean gotRail = false;
		CircuitElm volt = null;

		//System.out.println("analyzeCircuit - 1st step: look for voltage or ground element");
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			if (ce instanceof GroundElm) {
				gotGround = true;
				break;
			}
			if (ce instanceof RailElm)
				gotRail = true;
			if (volt == null && ce instanceof VoltageElm)
				volt = ce;
		}

		// if no ground, and no rails, then the voltage elm's first terminal
		// is ground
		if (!gotGround && volt != null && !gotRail) {
			CircuitNode cn = new CircuitNode();
			Point pt = volt.getPost(0);
			cn.setX(pt.x);
			cn.setY(pt.y);
			getNodeList().addElement(cn);
		} else {
			// otherwise allocate extra node for ground
			System.out.println("analyzeCircuit - 1st step: no voltage or ground element found");
			System.out.println("analyzeCircuit - 1st step: allocate extra node for ground");
			CircuitNode cn = new CircuitNode();
			cn.setX(cn.setY(-1));
			getNodeList().addElement(cn);
		}

		//System.out.println("analyzeCircuit - 2nd step: allocate nodes and voltage sources");
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			int inodes = ce.getInternalNodeCount();
			int ivs = ce.getVoltageSourceCount();
			int posts = ce.getPostCount();

			// System.out.println(ce.getClass() + " internalNodeCount: " + inodes + "
			// voltageSourceCount: " + ivs + " postCount: " + posts);

			// allocate a node for each post and match posts to nodes
			for (j = 0; j != posts; j++) {
				Point pt = ce.getPost(j);
				assert pt != null;
				int k;
				for (k = 0; k != getNodeList().size(); k++) {
					CircuitNode cn = getCircuitNode(k);
					if (pt.x == cn.getX() && pt.y == cn.getY())
						break;
				}
				if (k == getNodeList().size()) {
					CircuitNode cn = new CircuitNode();
					cn.setX(pt.x);
					cn.setY(pt.y);
					CircuitNodeLink cnl = new CircuitNodeLink();
					cnl.setNum(j);
					cnl.setElm(ce);
					cn.getLinks().addElement(cnl);
					ce.setNode(j, getNodeList().size());
					getNodeList().addElement(cn);
				} else {
					CircuitNodeLink cnl = new CircuitNodeLink();
					cnl.setNum(j);
					cnl.setElm(ce);
					getCircuitNode(k).getLinks().addElement(cnl);
					ce.setNode(j, k);
					// if it's the ground node, make sure the node voltage is 0,
					// cause it may not get set later
					if (k == 0)
						ce.setNodeVoltage(j, 0);
				}
			}
			for (j = 0; j != inodes; j++) {
				CircuitNode cn = new CircuitNode();
				cn.setX(cn.setY(-1));
				cn.setInternal(true);
				CircuitNodeLink cnl = new CircuitNodeLink();
				cnl.setNum(j + posts);
				cnl.setElm(ce);
				cn.getLinks().addElement(cnl);
				ce.setNode(cnl.getNum(), getNodeList().size());
				getNodeList().addElement(cn);
			}
			vscount += ivs;
		}
		// System.out.println("vscount: " + vscount);
		System.out.println("analyzeCircuit - 2nd step: #nodes for CirSim: " + getNodeList().size());
		for (int k = 0; k < getNodeList().size(); k++) {
			CircuitNode cn = getCircuitNode(k);
			//System.out.println("analyzeCircuit - 2nd step: Node{" + cn.getX() + ";" + cn.getY() + "}");
		}

		voltageSources = new CircuitElm[vscount];
		vscount = 0;
		circuitNonLinear = false;
		//System.out.println("analyzeCircuit - 3rd step: determine if circuit is nonlinear");
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			if (ce.nonLinear())
				circuitNonLinear = true;
			int ivs = ce.getVoltageSourceCount();
			for (j = 0; j != ivs; j++) {
				voltageSources[vscount] = ce;
				ce.setVoltageSource(j, vscount++);
			}
		}
		// System.out.println("vscount: " + vscount);
		voltageSourceCount = vscount;

		int matrixSize = getNodeList().size() - 1 + vscount;
		// System.out.println("matrixSize: " + matrixSize);
		circuitMatrix = new double[matrixSize][matrixSize];
		circuitRightSide = new double[matrixSize];
		origMatrix = new double[matrixSize][matrixSize];
		origRightSide = new double[matrixSize];
		circuitMatrixSize = circuitMatrixFullSize = matrixSize;
		circuitRowInfo = new RowInfo[matrixSize];
		circuitPermute = new int[matrixSize];
		for (i = 0; i != matrixSize; i++)
			circuitRowInfo[i] = new RowInfo();
		circuitNeedsMap = false;

		System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

		System.out.println("analyzeCircuit - 3rd step: init circuitMatrix and circuitRightSide");

		// stamp linear circuit elements
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			System.out.println("\nanalyzeCircuit - 3rd step: " + ce.getClass().getSimpleName() + " component");
			ce.stamp();
		}

		System.out.println("\ncircuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

		// System.out.println("ac4");
		System.out.println("analyzeCircuit - 4th step: determine nodes that are unconnected");
		// determine nodes that are unconnected
		boolean closure[] = new boolean[getNodeList().size()];
		boolean changed = true;
		closure[0] = true;
		while (changed) {
			changed = false;
			for (i = 0; i != getElmList().size(); i++) {
				CircuitElm ce = getElm(i);
				// loop through all ce's nodes to see if they are connected
				// to other nodes not in closure
				for (j = 0; j < ce.getPostCount(); j++) {
					if (!closure[ce.getNode(j)]) {
						if (ce.hasGroundConnection(j))
							closure[ce.getNode(j)] = changed = true;
						continue;
					}
					int k;
					for (k = 0; k != ce.getPostCount(); k++) {
						if (j == k)
							continue;
						int kn = ce.getNode(k);
						if (ce.getConnection(j, k) && !closure[kn]) {
							closure[kn] = true;
							changed = true;
						}
					}
				}
			}
			if (changed)
				continue;

			// connect unconnected nodes
			for (i = 0; i != getNodeList().size(); i++)
				if (!closure[i] && !getCircuitNode(i).isInternal()) {
					System.out.println("node " + i + " unconnected");
					System.out.println("analyzeCircuit - 4th step: Node{" + getCircuitNode(i).getX() + "; "
							+ getCircuitNode(i).getY() + "}");
					stampResistor(0, i, 1e8);
					closure[i] = true;
					changed = true;
					break;
				}
		}
		// System.out.println("ac5");
		System.out.println("analyzeCircuit - 5th step: check for circuit integrity");
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			// look for inductors with no current path
			if (ce instanceof InductorElm) {
				FindPathInfo fpi = new FindPathInfo(FindPathInfo.INDUCT, ce, ce.getNode(1));
				// first try findPath with maximum depth of 5, to avoid slowdowns
				if (!fpi.findPath(ce.getNode(0), 5) && !fpi.findPath(ce.getNode(0))) {
					System.out.println(ce + " no path");
					ce.reset();
				}
			}
			// look for current sources with no current path
			if (ce instanceof CurrentElm) {
				FindPathInfo fpi = new FindPathInfo(FindPathInfo.INDUCT, ce, ce.getNode(1));
				if (!fpi.findPath(ce.getNode(0))) {
					stop("No path for current source!", ce);
					return;
				}
			}
			// look for voltage source loops
			if ((ce instanceof VoltageElm && ce.getPostCount() == 2) || ce instanceof WireElm) {
				FindPathInfo fpi = new FindPathInfo(FindPathInfo.VOLTAGE, ce, ce.getNode(1));
				if (fpi.findPath(ce.getNode(0))) {
					stop("Voltage source/wire loop with no resistance!", ce);
					return;
				}
			}
			// look for shorted caps, or caps w/ voltage but no R
			if (ce instanceof CapacitorElm) {
				FindPathInfo fpi = new FindPathInfo(FindPathInfo.SHORT, ce, ce.getNode(1));
				if (fpi.findPath(ce.getNode(0))) {
					System.out.println(ce + " shorted");
					ce.reset();
				} else {
					fpi = new FindPathInfo(FindPathInfo.CAP_V, ce, ce.getNode(1));
					if (fpi.findPath(ce.getNode(0))) {
						stop("Capacitor loop with no resistance!", ce);
						return;
					}
				}
			}
		}

		System.out.println("circuitRowInfo: ");
		for (RowInfo ri : circuitRowInfo) {
			System.out.println(ri.toString());
		}

		// System.out.println("ac6");
		System.out.println("analyzeCircuit - 6th step: simplify the matrix");
		System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

		// simplify the matrix; this speeds things up quite a bit
		for (i = 0; i != matrixSize; i++) {
			int qm = -1, qp = -1;
			double qv = 0;
			RowInfo re = circuitRowInfo[i];
			/*
			 * System.out.println("row " + i + " " + re.lsChanges + " " + re.rsChanges + " "
			 * + re.dropRow);
			 */
			if (re.isLsChanges() || re.isDropRow() || re.isRsChanges())
				continue;
			double rsadd = 0;

			// look for rows that can be removed
			for (j = 0; j != matrixSize; j++) {
				double q = circuitMatrix[i][j];
				if (circuitRowInfo[j].getType() == RowInfo.ROW_CONST) {
					// keep a running total of const values that have been
					// removed already
					rsadd -= circuitRowInfo[j].getValue() * q;
					continue;
				}
				if (q == 0)
					continue;
				if (qp == -1) {
					qp = j;
					qv = q;
					continue;
				}
				if (qm == -1 && q == -qv) {
					qm = j;
					continue;
				}
				break;
			}
			// System.out.println("line " + i + " " + qp + " " + qm + " " + j);
			/*
			 * if (qp != -1 && circuitRowInfo[qp].lsChanges) {
			 * System.out.println("lschanges"); continue; } if (qm != -1 &&
			 * circuitRowInfo[qm].lsChanges) { System.out.println("lschanges"); continue; }
			 */
			if (j == matrixSize) {
				if (qp == -1) {
					stop("Matrix error", null);
					return;
				}
				RowInfo elt = circuitRowInfo[qp];
				if (qm == -1) {
					// we found a row with only one nonzero entry; that value
					// is a constant
					int k;
					for (k = 0; elt.getType() == RowInfo.ROW_EQUAL && k < 100; k++) {
						// follow the chain
						/*
						 * System.out.println("following equal chain from " + i + " " + qp + " to " +
						 * elt.nodeEq);
						 */
						qp = elt.getNodeEq();
						elt = circuitRowInfo[qp];
					}
					if (elt.getType() == RowInfo.ROW_EQUAL) {
						// break equal chains
						// System.out.println("Break equal chain");
						elt.setType(RowInfo.ROW_NORMAL);
						continue;
					}
					if (elt.getType() != RowInfo.ROW_NORMAL) {
						System.out.println("type already " + elt.getType() + " for " + qp + "!");
						continue;
					}
					elt.setType(RowInfo.ROW_CONST);
					elt.setValue((circuitRightSide[i] + rsadd) / qv);
					circuitRowInfo[i].setDropRow(true);
					// System.out.println(qp + " * " + qv + " = const " + elt.value);
					i = -1; // start over from scratch
				} else if (circuitRightSide[i] + rsadd == 0) {
					// we found a row with only two nonzero entries, and one
					// is the negative of the other; the values are equal
					if (elt.getType() != RowInfo.ROW_NORMAL) {
						// System.out.println("swapping");
						int qq = qm;
						qm = qp;
						qp = qq;
						elt = circuitRowInfo[qp];
						if (elt.getType() != RowInfo.ROW_NORMAL) {
							// we should follow the chain here, but this
							// hardly ever happens so it's not worth worrying
							// about
							System.out.println("swap failed");
							continue;
						}
					}
					elt.setType(RowInfo.ROW_EQUAL);
					elt.setNodeEq(qm);
					circuitRowInfo[i].setDropRow(true);
					// System.out.println(qp + " = " + qm);
				}
			}
		}

		System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));
		// System.out.println("ac7");
		System.out.println("analyzeCircuit - 7th step: find size of new matrix");
		// find size of new matrix
		int nn = 0;
		for (i = 0; i != matrixSize; i++) {
			RowInfo elt = circuitRowInfo[i];
			if (elt.getType() == RowInfo.ROW_NORMAL) {
				elt.setMapCol(nn++);
				// System.out.println("col " + i + " maps to " + elt.mapCol);
				continue;
			}
			if (elt.getType() == RowInfo.ROW_EQUAL) {
				RowInfo e2 = null;
				// resolve chains of equality; 100 max steps to avoid loops
				for (j = 0; j != 100; j++) {
					e2 = circuitRowInfo[elt.getNodeEq()];
					if (e2.getType() != RowInfo.ROW_EQUAL)
						break;
					if (i == e2.getNodeEq())
						break;
					elt.setNodeEq(e2.getNodeEq());
				}
			}
			if (elt.getType() == RowInfo.ROW_CONST)
				elt.setMapCol(-1);
		}
		for (i = 0; i != matrixSize; i++) {
			RowInfo elt = circuitRowInfo[i];
			if (elt.getType() == RowInfo.ROW_EQUAL) {
				RowInfo e2 = circuitRowInfo[elt.getNodeEq()];
				if (e2.getType() == RowInfo.ROW_CONST) {
					// if something is equal to a const, it's a const
					elt.setType(e2.getType());
					elt.setValue(e2.getValue());
					elt.setMapCol(-1);
					// System.out.println(i + " = [late]const " + elt.value);
				} else {
					elt.setMapCol(e2.getMapCol());
					// System.out.println(i + " maps to: " + e2.mapCol);
				}
			}
		}
		// System.out.println("ac8");
		System.out.println("analyzeCircuit - 8th step: creation of new matrix");
		System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));
		/*
		 * System.out.println("matrixSize = " + matrixSize);
		 * 
		 * for (j = 0; j != circuitMatrixSize; j++) { System.out.println(j + ": "); for
		 * (i = 0; i != circuitMatrixSize; i++) System.out.print(circuitMatrix[j][i] +
		 * " "); System.out.print("  " + circuitRightSide[j] + "\n"); }
		 * System.out.print("\n");
		 */

		// make the new, simplified matrix
		int newsize = nn;
		double newmatx[][] = new double[newsize][newsize];
		double newrs[] = new double[newsize];
		int ii = 0;
		for (i = 0; i != matrixSize; i++) {
			RowInfo rri = circuitRowInfo[i];
			if (rri.isDropRow()) {
				rri.setMapRow(-1);
				continue;
			}
			newrs[ii] = circuitRightSide[i];
			rri.setMapRow(ii);
			// System.out.println("Row " + i + " maps to " + ii);
			for (j = 0; j != matrixSize; j++) {
				RowInfo ri = circuitRowInfo[j];
				if (ri.getType() == RowInfo.ROW_CONST)
					newrs[ii] -= ri.getValue() * circuitMatrix[i][j];
				else
					newmatx[ii][ri.getMapCol()] += circuitMatrix[i][j];
			}
			ii++;
		}

		circuitMatrix = newmatx;
		circuitRightSide = newrs;
		matrixSize = circuitMatrixSize = newsize;
		for (i = 0; i != matrixSize; i++)
			origRightSide[i] = circuitRightSide[i];
		for (i = 0; i != matrixSize; i++)
			for (j = 0; j != matrixSize; j++)
				origMatrix[i][j] = circuitMatrix[i][j];
		circuitNeedsMap = true;

		System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
		System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

		/*
		 * System.out.println("matrixSize = " + matrixSize + " " + circuitNonLinear);
		 * for (j = 0; j != circuitMatrixSize; j++) { for (i = 0; i !=
		 * circuitMatrixSize; i++) System.out.print(circuitMatrix[j][i] + " ");
		 * System.out.print("  " + circuitRightSide[j] + "\n"); }
		 * System.out.print("\n");
		 */

		// if a matrix is linear, we can do the lu_factor here instead of
		// needing to do it every frame
		if (!circuitNonLinear) {
			if (!lu_factor(circuitMatrix, circuitMatrixSize, circuitPermute)) {
				stop("Singular matrix!", null);
				return;
			}
		}
	}

	class FindPathInfo {
		static final int INDUCT = 1;
		static final int VOLTAGE = 2;
		static final int SHORT = 3;
		static final int CAP_V = 4;
		boolean used[];
		int dest;
		CircuitElm firstElm;
		int type;

		FindPathInfo(int t, CircuitElm e, int d) {
			dest = d;
			type = t;
			firstElm = e;
			used = new boolean[getNodeList().size()];
		}

		boolean findPath(int n1) {
			return findPath(n1, -1);
		}

		boolean findPath(int n1, int depth) {
			if (n1 == dest)
				return true;
			if (depth-- == 0)
				return false;
			if (used[n1]) {
				// System.out.println("used " + n1);
				return false;
			}
			used[n1] = true;
			int i;
			for (i = 0; i != getElmList().size(); i++) {
				CircuitElm ce = getElm(i);
				if (ce == firstElm)
					continue;
				if (type == INDUCT) {
					if (ce instanceof CurrentElm)
						continue;
				}
				if (type == VOLTAGE) {
					if (!(ce.isWire() || ce instanceof VoltageElm))
						continue;
				}
				if (type == SHORT && !ce.isWire())
					continue;
				if (type == CAP_V) {
					if (!(ce.isWire() || ce instanceof CapacitorElm || ce instanceof VoltageElm))
						continue;
				}
				if (n1 == 0) {
					// look for posts which have a ground connection;
					// our path can go through ground
					int j;
					for (j = 0; j != ce.getPostCount(); j++)
						if (ce.hasGroundConnection(j) && findPath(ce.getNode(j), depth)) {
							used[n1] = false;
							return true;
						}
				}
				int j;
				for (j = 0; j != ce.getPostCount(); j++) {
					// System.out.println(ce + " " + ce.getNode(j));
					if (ce.getNode(j) == n1)
						break;
				}
				if (j == ce.getPostCount())
					continue;
				if (ce.hasGroundConnection(j) && findPath(0, depth)) {
					// System.out.println(ce + " has ground");
					used[n1] = false;
					return true;
				}
				if (type == INDUCT && ce instanceof InductorElm) {
					double c = ce.getCurrent();
					if (j == 0)
						c = -c;
					// System.out.println("matching " + c + " to " + firstElm.getCurrent());
					// System.out.println(ce + " " + firstElm);
					if (Math.abs(c - firstElm.getCurrent()) > 1e-10)
						continue;
				}
				int k;
				for (k = 0; k != ce.getPostCount(); k++) {
					if (j == k)
						continue;
					// System.out.println(ce + " " + ce.getNode(j) + "-" + ce.getNode(k));
					if (ce.getConnection(j, k) && findPath(ce.getNode(k), depth)) {
						// System.out.println("got findpath " + n1);
						used[n1] = false;
						return true;
					}
					// System.out.println("back on findpath " + n1);
				}
			}
			used[n1] = false;
			// System.out.println(n1 + " failed");
			return false;
		}
	}

	public void stop(String s, CircuitElm ce) {
		stopMessage = s;
		circuitMatrix = null;
		stopElm = ce;
		setAnalyzeFlag(false);
	}

	// control voltage source vs with voltage from n1 to n2 (must
	// also call stampVoltageSource())
	public void stampVCVS(int n1, int n2, double coef, int vs) {
		int vn = getNodeList().size() + vs;
		stampMatrix(vn, n1, coef);
		stampMatrix(vn, n2, -coef);
	}

	/**
	 * Stamp independent voltage source #vs, from n1 to n2, amount v
	 * 
	 * @param n1
	 * @param n2
	 * @param vs
	 * @param v
	 */
	public void stampVoltageSource(int n1, int n2, int vs, double v) {
		int vn = getNodeList().size() + vs;
		stampMatrix(vn, n1, -1);
		stampMatrix(vn, n2, 1);
		stampRightSide(vn, v);
		stampMatrix(n1, vn, 1);
		stampMatrix(n2, vn, -1);
	}

	// use this if the amount of voltage is going to be updated in doStep()
	public void stampVoltageSource(int n1, int n2, int vs) {
		int vn = getNodeList().size() + vs;
		stampMatrix(vn, n1, -1);
		stampMatrix(vn, n2, 1);
		stampRightSide(vn);
		stampMatrix(n1, vn, 1);
		stampMatrix(n2, vn, -1);
	}

	public void updateVoltageSource(int n1, int n2, int vs, double v) {
		int vn = getNodeList().size() + vs;
		stampRightSide(vn, v);
	}

	public void stampResistor(int n1, int n2, double r) {
		assert r != 0;
		double r0 = 1 / r;
		if (Double.isNaN(r0) || Double.isInfinite(r0)) {
			System.out.print("bad resistance " + r + " " + r0 + "\n");
			int a = 0;
			a /= a;
		}

		System.out.println("nodes[0]: " + n1 + " nodes[1]: " + n2 + " r0: " + r0);
		stampMatrix(n1, n1, r0);
		stampMatrix(n2, n2, r0);
		stampMatrix(n1, n2, -r0);
		stampMatrix(n2, n1, -r0);
	}

	public void stampConductance(int n1, int n2, double r0) {
		stampMatrix(n1, n1, r0);
		stampMatrix(n2, n2, r0);
		stampMatrix(n1, n2, -r0);
		stampMatrix(n2, n1, -r0);
	}

	// current from cn1 to cn2 is equal to voltage from vn1 to 2, divided by g
	public void stampVCCurrentSource(int cn1, int cn2, int vn1, int vn2, double g) {
		stampMatrix(cn1, vn1, g);
		stampMatrix(cn2, vn2, g);
		stampMatrix(cn1, vn2, -g);
		stampMatrix(cn2, vn1, -g);
	}

	public void stampCurrentSource(int n1, int n2, double i) {
		stampRightSide(n1, -i);
		stampRightSide(n2, i);
	}

	// stamp a current source from n1 to n2 depending on current through vs
	public void stampCCCS(int n1, int n2, int vs, double gain) {
		int vn = getNodeList().size() + vs;
		stampMatrix(n1, vn, gain);
		stampMatrix(n2, vn, -gain);
	}

	// stamp value x in row i, column j, meaning that a voltage change
	// of dv in node j will increase the current into node i by x dv.
	// (Unless i or j is a voltage source node.)
	public void stampMatrix(int i, int j, double x) {
		if (i > 0 && j > 0) {
			if (circuitNeedsMap) {
				i = circuitRowInfo[i - 1].getMapRow();
				RowInfo ri = circuitRowInfo[j - 1];
				if (ri.getType() == RowInfo.ROW_CONST) {
					// System.out.println("Stamping constant " + i + " " + j + " " + x);
					circuitRightSide[i] -= x * ri.getValue();
					return;
				}
				j = ri.getMapCol();
				// System.out.println("stamping " + i + " " + j + " " + x);
			} else {
				i--;
				j--;
			}
			System.out.print("circuitMatrix[" + i + "][" + j + "]: " + circuitMatrix[i][j] + " + x = " + x + " --> ");

			circuitMatrix[i][j] += x;

			System.out.println("circuitMatrix[" + i + "][" + j + "]: " + circuitMatrix[i][j]);
		}
	}

	/**
	 * Stamp value x on the right side of row i, representing an independent current
	 * source flowing into node i
	 */
	public void stampRightSide(int i, double x) {
		if (i > 0) {
			if (circuitNeedsMap) {
				i = circuitRowInfo[i - 1].getMapRow();
				// System.out.println("stamping " + i + " " + x);
			} else
				i--;

			System.out.print("circuitRightSide[" + i + "]: " + circuitRightSide[i] + " + x = " + x + " --> ");

			circuitRightSide[i] += x;

			System.out.println("circuitRightSide[" + i + "]: " + circuitRightSide[i]);
		}
	}

	/**
	 * Indicate that the value on the right side of row i changes in doStep()
	 * 
	 * @param i
	 */
	public void stampRightSide(int i) {
		// System.out.println("rschanges true " + (i-1));

		if (i > 0) {
			System.out.print("circuitRowInfo[" + (i - 1) + "]: " + circuitRowInfo[i - 1] + " --> ");
			circuitRowInfo[i - 1].setRsChanges(true);
			System.out.println("circuitRowInfo[" + (i - 1) + "]: " + circuitRowInfo[i - 1]);
		}

	}

	// indicate that the values on the left side of row i change in doStep()
	public void stampNonLinear(int i) {
		if (i > 0)
			circuitRowInfo[i - 1].setLsChanges(true);
	}

	private boolean converged;
	private int subIterations;

	// do a step and continue if true
	public boolean loopAndContinue(boolean debugprint) {
		int i, j, k, subiter;

		System.out.println("loopAndContinue - 1st step: start iteration");
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			ce.startIteration();
		}

		steps++;
		final int subiterCount = 5000;
		System.out.println("loopAndContinue - 2nd step: doStep");
		for (subiter = 0; subiter != subiterCount; subiter++) {
			setConverged(true);
			setSubIterations(subiter);

			for (i = 0; i != circuitMatrixSize; i++)
				circuitRightSide[i] = origRightSide[i];

			if (circuitNonLinear) {
				for (i = 0; i != circuitMatrixSize; i++)
					for (j = 0; j != circuitMatrixSize; j++)
						circuitMatrix[i][j] = origMatrix[i][j];
			}

			for (i = 0; i != getElmList().size(); i++) {
				CircuitElm ce = getElm(i);
				ce.doStep();
			}

			if (stopMessage != null)
				return false;

			boolean printit = debugprint;
			debugprint = false;

			// check circuitMatrix for invalid values
			for (j = 0; j != circuitMatrixSize; j++) {
				for (i = 0; i != circuitMatrixSize; i++) {
					double x = circuitMatrix[i][j];
					if (Double.isNaN(x) || Double.isInfinite(x)) {
						stop("nan/infinite matrix!", null);
						return false;
					}
				}
			}

			// print circuitMatrix | circuitRightSide
			if (printit) {
				for (j = 0; j != circuitMatrixSize; j++) {
					for (i = 0; i != circuitMatrixSize; i++)
						System.out.print(circuitMatrix[j][i] + ",");
					System.out.print("  " + circuitRightSide[j] + "\n");
				}
				System.out.print("\n");
			}

			if (circuitNonLinear) {
				if (isConverged() && subiter > 0)
					break;
				if (!lu_factor(circuitMatrix, circuitMatrixSize, circuitPermute)) {
					stop("Singular matrix!", null);
					return false;
				}
			}

			System.out.println("loopAndContinue - 3rd step: lu_solve");
			System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
			System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

			lu_solve(circuitMatrix, circuitMatrixSize, circuitPermute, circuitRightSide);

			System.out.println("circuitMatrix: " + Arrays.deepToString(circuitMatrix));
			System.out.println("circuitRightSide: " + Arrays.toString(circuitRightSide));

			for (j = 0; j != circuitMatrixFullSize; j++) {
				RowInfo ri = circuitRowInfo[j];
				double res = 0;
				if (ri.getType() == RowInfo.ROW_CONST)
					res = ri.getValue();
				else
					res = circuitRightSide[ri.getMapCol()];

				System.out.println("\nj = " + j + ", res = " + res + ", type = " + ri.getType() + ", mapCol = " + ri.getMapCol());

				if (Double.isNaN(res)) {
					setConverged(false);
					// debugprint = true;
					break;
				}
				
				if (j < getNodeList().size() - 1) {
					CircuitNode cn = getCircuitNode(j + 1);
					for (k = 0; k != cn.getLinks().size(); k++) {
						CircuitNodeLink cnl = cn.getLinks().elementAt(k);
						cnl.getElm().setNodeVoltage(cnl.getNum(), res);
					}
				} else {
					int ji = j - (getNodeList().size() - 1);
					// System.out.println("setting vsrc " + ji + " to " + res);
					voltageSources[ji].setCurrent(ji, res);
				}
			}

			if (!circuitNonLinear)
				break;
		}
		
		if (subiter > 5)
			System.out.print("converged after " + subiter + " iterations\n");
		if (subiter == subiterCount) {
			stop("Convergence failed!", null);
			// break;
			return false;
		}
		
		System.out.println("\nt = " + getT() + ", timeStep = " + getTimeStep());
		setT(getT() + getTimeStep());
		System.out.println("t = " + getT() + ", timeStep = " + getTimeStep());
		
		for (i = 0; i != scopeCount; i++)
			scopes[i].timeStep();
		// continue
		return true;
	}

	int min(int a, int b) {
		return (a < b) ? a : b;
	}

	int max(int a, int b) {
		return (a > b) ? a : b;
	}

	void stackScope(int s) {
		if (s == 0) {
			if (scopeCount < 2)
				return;
			s = 1;
		}
		if (scopes[s].getPosition() == scopes[s - 1].getPosition())
			return;
		scopes[s].setPosition(scopes[s - 1].getPosition());
		for (s++; s < scopeCount; s++)
			scopes[s].setPosition(scopes[s].getPosition() - 1);
	}

	void unstackScope(int s) {
		if (s == 0) {
			if (scopeCount < 2)
				return;
			s = 1;
		}
		if (scopes[s].getPosition() != scopes[s - 1].getPosition())
			return;
		for (; s < scopeCount; s++)
			scopes[s].setPosition(scopes[s].getPosition() + 1);
	}

	void stackAll() {
		int i;
		for (i = 0; i != scopeCount; i++) {
			scopes[i].setPosition(0);
			scopes[i].setShowMax(scopes[i].setShowMin(false));
		}
	}

	void unstackAll() {
		int i;
		for (i = 0; i != scopeCount; i++) {
			scopes[i].setPosition(i);
			scopes[i].setShowMax(true);
		}
	}

	public int snapGrid(int x) {
		return (x + gridRound) & gridMask;
	}

	public int locateElm(CircuitElm elm) {
		int i;
		for (i = 0; i != getElmList().size(); i++)
			if (elm == getElmList().get(i))
				return i;
		return -1;
	}

	void removeZeroLengthElements() {
		int i;
		boolean changed = false;
		for (i = getElmList().size() - 1; i >= 0; i--) {
			CircuitElm ce = getElm(i);
			if (ce.getX() == ce.getX2() && ce.getY() == ce.getY2()) {
				getElmList().remove(i);
				ce.delete();
				changed = true;
			}
		}
	}

	int distanceSq(int x1, int y1, int x2, int y2) {
		x2 -= x1;
		y2 -= y1;
		return x2 * x2 + y2 * y2;
	}

	void setMenuSelection() {
		if (menuElm != null) {
			if (menuElm.selected)
				return;
			clearSelection();
			menuElm.setSelected(true);
		}
	}

	void clearSelection() {
		int i;
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			ce.setSelected(false);
		}
	}

	void doSelectAll() {
		int i;
		for (i = 0; i != getElmList().size(); i++) {
			CircuitElm ce = getElm(i);
			ce.setSelected(true);
		}
	}
	
	// factors a matrix into upper and lower triangular matrices by
	// gaussian elimination. On entry, a[0..n-1][0..n-1] is the
	// matrix to be factored. ipvt[] returns an integer vector of pivot
	// indices, used in the lu_solve() routine.
	boolean lu_factor(double a[][], int n, int ipvt[]) {
		double scaleFactors[];
		int i, j, k;

		scaleFactors = new double[n];

		// divide each row by its largest element, keeping track of the
		// scaling factors
		for (i = 0; i != n; i++) {
			double largest = 0;
			for (j = 0; j != n; j++) {
				double x = Math.abs(a[i][j]);
				if (x > largest)
					largest = x;
			}
			// if all zeros, it's a singular matrix
			if (largest == 0)
				return false;
			scaleFactors[i] = 1.0 / largest;
		}

		// use Crout's method; loop through the columns
		for (j = 0; j != n; j++) {

			// calculate upper triangular elements for this column
			for (i = 0; i != j; i++) {
				double q = a[i][j];
				for (k = 0; k != i; k++)
					q -= a[i][k] * a[k][j];
				a[i][j] = q;
			}

			// calculate lower triangular elements for this column
			double largest = 0;
			int largestRow = -1;
			for (i = j; i != n; i++) {
				double q = a[i][j];
				for (k = 0; k != j; k++)
					q -= a[i][k] * a[k][j];
				a[i][j] = q;
				double x = Math.abs(q);
				if (x >= largest) {
					largest = x;
					largestRow = i;
				}
			}

			// pivoting
			if (j != largestRow) {
				double x;
				for (k = 0; k != n; k++) {
					x = a[largestRow][k];
					a[largestRow][k] = a[j][k];
					a[j][k] = x;
				}
				scaleFactors[largestRow] = scaleFactors[j];
			}

			// keep track of row interchanges
			ipvt[j] = largestRow;

			// avoid zeros
			if (a[j][j] == 0.0) {
				System.out.println("avoided zero");
				a[j][j] = 1e-18;
			}

			if (j != n - 1) {
				double mult = 1.0 / a[j][j];
				for (i = j + 1; i != n; i++)
					a[i][j] *= mult;
			}
		}
		return true;
	}

	/**
	 * Solves the set of n linear equations using a LU factorization previously
	 * performed by lu_factor. On input, b[0..n-1] is the right hand side of the
	 * equations, and on output, contains the solution.
	 * 
	 * @param a
	 * @param n
	 * @param ipvt
	 * @param b
	 */
	void lu_solve(double a[][], int n, int ipvt[], double b[]) {
		int i;

		// find first nonzero b element
		for (i = 0; i != n; i++) {
			int row = ipvt[i];

			double swap = b[row];
			b[row] = b[i];
			b[i] = swap;
			if (swap != 0)
				break;
		}

		int bi = i++;
		for (; i < n; i++) {
			int row = ipvt[i];
			int j;
			double tot = b[row];

			b[row] = b[i];
			// forward substitution using the lower triangular matrix
			for (j = bi; j < i; j++)
				tot -= a[i][j] * b[j];
			b[i] = tot;
		}
		for (i = n - 1; i >= 0; i--) {
			double tot = b[i];

			// back-substitution using the upper triangular matrix
			int j;
			for (j = i + 1; j != n; j++)
				tot -= a[i][j] * b[j];
			b[i] = tot / a[i][i];
		}
	}

	public double getTimeStep() {
		assert timeStep > 0;
		return timeStep;
	}

	public void setTimeStep(double timeStep) {
		this.timeStep = timeStep;
	}

	public int getScopeSelected() {
		return scopeSelected;
	}

	public void setScopeSelected(int scopeSelected) {
		this.scopeSelected = scopeSelected;
	}

	public CircuitElm getMouseElm() {
		return mouseElm;
	}

	public void setMouseElm(CircuitElm mouseElm) {
		this.mouseElm = mouseElm;
	}

	public double getT() {
		return t;
	}

	public void setT(double t) {
		this.t = t;
	}

	public CircuitElm getPlotXElm() {
		return plotXElm;
	}

	public CircuitElm setPlotXElm(CircuitElm plotXElm) {
		this.plotXElm = plotXElm;
		return plotXElm;
	}

	public CircuitElm getPlotYElm() {
		return plotYElm;
	}

	public CircuitElm setPlotYElm(CircuitElm plotYElm) {
		this.plotYElm = plotYElm;
		return plotYElm;
	}

	public boolean isUseBufferedImage() {
		return useBufferedImage;
	}

	public void setUseBufferedImage(boolean useBufferedImage) {
		this.useBufferedImage = useBufferedImage;
	}

	public List<CircuitElm> getElmList() {
		return elmList;
	}

	public void setElmList(List<CircuitElm> elmList) {
		this.elmList = elmList;
	}

	public CircuitElm getDragElm() {
		return dragElm;
	}

	public void setDragElm(CircuitElm dragElm) {
		this.dragElm = dragElm;
	}

	public int getMouseMode() {
		return mouseMode;
	}

	public static String getMuString() {
		return muString;
	}

	public static void setMuString(String muString) {
		CirSim.muString = muString;
	}

	public static ImportExportDialog getImpDialog() {
		return impDialog;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public boolean isConverged() {
		return converged;
	}

	public void setConverged(boolean converged) {
		this.converged = converged;
	}

	public Vector<CircuitNode> getNodeList() {
		return nodeList;
	}

	public void setNodeList(Vector<CircuitNode> nodeList) {
		this.nodeList = nodeList;
	}

	public static String getOhmString() {
		return ohmString;
	}

	public int getSubIterations() {
		return subIterations;
	}

	public void setSubIterations(int subIterations) {
		this.subIterations = subIterations;
	}

	public boolean isAnalyzeFlag() {
		return analyzeFlag;
	}

	public void setAnalyzeFlag(boolean analyzeFlag) {
		this.analyzeFlag = analyzeFlag;
	}

	public String toString() {
		// TODO
		return new ToStringBuilder(this).append("elmList", elmList).append("nodeList", nodeList).toString();
	}

}
