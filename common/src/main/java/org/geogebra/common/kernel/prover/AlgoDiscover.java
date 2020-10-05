package org.geogebra.common.kernel.prover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.javax.swing.RelationPane;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Path;
import org.geogebra.common.kernel.algos.AlgoCircleThreePoints;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.AlgoIntersectLineConic;
import org.geogebra.common.kernel.algos.AlgoIntersectLines;
import org.geogebra.common.kernel.algos.AlgoIntersectSingle;
import org.geogebra.common.kernel.algos.AlgoJoinPoints;
import org.geogebra.common.kernel.algos.AlgoJoinPointsSegment;
import org.geogebra.common.kernel.algos.AlgoMidpoint;
import org.geogebra.common.kernel.algos.AlgoMidpointSegment;
import org.geogebra.common.kernel.algos.AlgoOrthoLinePointLine;
import org.geogebra.common.kernel.algos.AlgoPointOnPath;
import org.geogebra.common.kernel.cas.UsesCAS;
import org.geogebra.common.kernel.commands.Commands;
import org.geogebra.common.kernel.geos.GProperty;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoConic;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoList;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.discovery.Circle;
import org.geogebra.common.kernel.prover.discovery.EqualLongSegments;
import org.geogebra.common.kernel.prover.discovery.Line;
import org.geogebra.common.kernel.prover.discovery.ParallelLines;
import org.geogebra.common.kernel.prover.discovery.Point;
import org.geogebra.common.kernel.prover.discovery.Pool;
import org.geogebra.common.kernel.prover.discovery.Segment;
import org.geogebra.common.main.Localization;
import org.geogebra.common.plugin.EuclidianStyleConstants;
import org.geogebra.common.plugin.Event;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.util.StringUtil;
import org.geogebra.common.util.debug.Log;

/**
 * @author Zoltan Kovacs <zoltan@geogebra.org>
 */

// This is actually not an algo. Consider putting this entirely in CmdDiscover.
public class AlgoDiscover extends AlgoElement implements UsesCAS {

    private GeoElement input; // input
    private GeoElement output; // output, actually null

    private HashSet<Line> drawnLines = new HashSet<>();
    private HashSet<Circle> drawnCircles = new HashSet<>();
    private HashSet<ParallelLines> drawnDirections = new HashSet<>();
    private HashSet<EqualLongSegments> drawnSegments = new HashSet<>();

    public AlgoDiscover(final Construction cons,
                        final GeoElement input) {
        super(cons);
        setPrintedInXML(false);
        this.input = input;
        this.output = null;

        initialCompute();
        setInputOutput();
    }

    @Override
    public Commands getClassName() {
        return Commands.Discover;
    }

    @Override
    protected void setInputOutput() {
        super.input = new GeoElement[1];
        super.input[0] = this.input;
        super.setOutputLength(0);
        setDependencies(); // done by AlgoElement
    }

    public GeoElement getResult() {
        return this.output;
    }

    @Override
    public final void compute() { // do nothing for the moment
    }

    /*
     * Build the whole database of properties,
     * including all points in the construction list.
     */
    private boolean detectProperties(GeoPoint p) {
        HashSet<GeoElement> ges = new HashSet<>();
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            ges.add(ge);
        }

        int i = 0;
        for (GeoElement ge : ges) {
            if (ge instanceof GeoPoint && !p.equals(ge)) {
                if (!collectIdenticalPoints((GeoPoint) ge, false)) {
                    return false;
                }
                i++;
            }
        }
        if (i == 0) {
            // Only one point exists. No discovery will be done.
            return false;
        }

        detectOrthogonalCollinearities();
        Pool discoveryPool = cons.getDiscoveryPool();
        for (Point pp : discoveryPool.points) {
            if (!p.equals(pp.getGeoPoint())) {
                collectCollinearites(pp, false);
                collectConcyclicities(pp, false);
            }
        }
        collectCollinearites(discoveryPool.getPoint(p), true);
        collectConcyclicities(discoveryPool.getPoint(p), true);

        for (Point pp : discoveryPool.points) {
            if (!p.equals(pp.getGeoPoint())) {
                collectParallelisms(pp, false);
                collectEqualLongSegments(pp, false);
            }
        }
        collectParallelisms(discoveryPool.getPoint(p), true);
        collectEqualLongSegments(discoveryPool.getPoint(p), true);
        return true;
    }

    /*
     * Extend the database of identical points by
     * collecting all of them for a given input.
     */
    private boolean collectIdenticalPoints(GeoPoint p0, boolean discover) {
        Pool discoveryPool = cons.getDiscoveryPool();

        HashSet<GeoPoint> prevPoints = new HashSet<GeoPoint>();
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoPoint && !ge.equals(p0)) {
                prevPoints.add((GeoPoint) ge);
                }
            }

        Iterator<GeoPoint> pointPairs = prevPoints.iterator();

        while (pointPairs.hasNext()) {
            GeoPoint p1 = pointPairs.next();
            if (!discoveryPool.areIdentical(p0, p1)) {
                checkIdenticality(p0, p1);
            }
            if (!discoveryPool.areIdentical(p0, p1)) {
                discoveryPool.addPoint(p0);
                discoveryPool.addPoint(p1);
            }
        }

        // Second round.
        pointPairs = prevPoints.iterator();
        while (pointPairs.hasNext()) {
            GeoPoint p1 = pointPairs.next();
            if (!discoveryPool.areIdentical(p0, p1)) {
                // Conjecture: Indentical points (always, no numerical check is done!)
                AlgoAreEqual aac = new AlgoAreEqual(cons, p0, p1);
                GeoElement root = new GeoBoolean(cons);
                root.setParentAlgorithm(aac);
                AlgoProveDetails ap = new AlgoProveDetails(cons, root);
                ap.compute();
                GeoElement[] o = ap.getOutput();
                GeoList output = (GeoList) o[0];
                if (output.size() > 0) {
                    GeoElement truth = output.get(0);
                    if (((GeoBoolean) truth).getBoolean()) {
                        // Theorem: Identical points
                        discoveryPool.addIdenticality(p0, p1).setTrivial(false);
                    }
                } else {
                    // Here we don't know anything about the equality of the points.
                    // So we need to say goodbye to be on the safe side and exit Discover.
                    return false;
                }
                ap.remove();
                aac.remove();
            }
        }

        return true;

    }

    /*
     * Extend the database of collinearities by
     * collecting all of them for a given input.
     */
    private void collectCollinearites(Point p0, boolean discover) {
        Pool discoveryPool = cons.getDiscoveryPool();

        HashSet<Point> prevPoints = new HashSet<Point>();
        for (Point ge : discoveryPool.points) {
            if (!ge.equals(p0)) {
                prevPoints.add(ge);
            }
        }

        Combinations lines = new Combinations(prevPoints, 2);

        while (lines.hasNext()) {
            Set<Point> line = lines.next();
            Iterator<Point> i = line.iterator();
            Point p1 = i.next();
            Point p2 = i.next();
            if (!discoveryPool.areCollinear(p0, p1, p2)) {
                // Add {p0,p1,p2} to the trivial pool if they are trivially collinear:
                checkCollinearity(p0, p1, p2);
                checkCollinearity(p1, p2, p0);
                checkCollinearity(p2, p0, p1);
            }
            if (!discoveryPool.areCollinear(p0, p1, p2)) {
                discoveryPool.addLine(p1, p2);
                discoveryPool.addLine(p0, p1);
                discoveryPool.addLine(p0, p2);
            }
        }

        // Second round:
        // put non-trivial collinearities in the
        // discovery pool. It is needed to do this for all p0 (not for just the final
        // one to discover) in order to have all parallel lines correctly.
        lines = new Combinations(prevPoints, 2);
        while (lines.hasNext()) {
            Set<Point> line = lines.next();
            Iterator<Point> i = line.iterator();
            Point p1 = i.next();
            Point p2 = i.next();
            if (!discoveryPool.areCollinear(p0, p1, p2)) {
                AlgoAreCollinear aac = new AlgoAreCollinear(cons, p0.getGeoPoint(), p1.getGeoPoint(), p2.getGeoPoint());
                if (aac.getResult().getBoolean()) {
                    // Conjecture: Collinearity
                    GeoElement root = new GeoBoolean(cons);
                    root.setParentAlgorithm(aac);
                    AlgoProveDetails ap = new AlgoProveDetails(cons, root);
                    ap.compute();
                    GeoElement[] o = ap.getOutput();
                    GeoList output = (GeoList) o[0];
                    if (output.size() > 0) {
                        GeoElement truth = output.get(0);
                        if (((GeoBoolean) truth).getBoolean()) {
                            // Theorem: Collinearity
                            discoveryPool.addCollinearity(p0, p1, p2).setTrivial(false);
                        }
                    }
                    ap.remove();
                }
                aac.remove();
            }
        }

        if (p0.getGeoPoint().getKernel().isSilentMode()) {
            return;
        }

        if (discover) {
            // Third round: Draw lines from the discovery pool
            // (those that are not yet drawn):
            for (Line l : discoveryPool.lines) {
                if (l.isTheorem()) {
                    if (l.getPoints().contains(p0)) {
                        if (!alreadyDrawn(l)) {
                            GeoPoint[] twopoints = l.getPoints2();
                            l.setGeoLine(addOutputLine(twopoints[0], twopoints[1]));
                        }
                        drawnLines.add(l);
                    }
                }
            }
        }
    }


    /*
     * Extend the database by
     * collecting all conclicities for a given input.
     */
    private void collectConcyclicities(Point p0, boolean discover) {
        Pool discoveryPool = cons.getDiscoveryPool();

        HashSet<Point> prevPoints = new HashSet<Point>();
        for (Point ge : discoveryPool.points) {
            if (!ge.equals(p0)) {
                prevPoints.add(ge);
            }
        }

        Combinations circles = new Combinations(prevPoints, 3);

        while (circles.hasNext()) {
            Set<Point> circle = circles.next();
            Iterator<Point> i = circle.iterator();
            Point p1 = i.next();
            Point p2 = i.next();
            Point p3 = i.next();
            // In case 3 of the 4 are collinear, let's ignore this case:
            if (are3Collinear(p0, p1, p2, p3)) {
                continue;
            }

            if (!discoveryPool.areConcyclic(p0, p1, p2, p3)) {
                // Add {p0,p1,p2,p3} to the trivial pool if they are trivially concyclic:
                checkConcyclicity(p0, p1, p2, p3);
                checkConcyclicity(p1, p2, p3, p0);
                checkConcyclicity(p2, p3, p0, p1);
                checkConcyclicity(p3, p0, p1, p2);
            }
            if (!discoveryPool.areConcyclic(p0, p1, p2, p3)) {
                discoveryPool.addCircle(p0, p1, p2);
                discoveryPool.addCircle(p0, p1, p3);
                discoveryPool.addCircle(p0, p2, p3);
                discoveryPool.addCircle(p1, p2, p3);
            }
        }

        if (discover) {
            // Second round:
            // put non-trivial concyclicities in the
            // discovery pool.
            circles = new Combinations(prevPoints, 3);
            while (circles.hasNext()) {
                Set<Point> circle = circles.next();
                Iterator<Point> i = circle.iterator();
                Point p1 = i.next();
                Point p2 = i.next();
                Point p3 = i.next();
                if (!are3Collinear(p0, p1, p2, p3) &&
                        !discoveryPool.areConcyclic(p0, p1, p2, p3)) {
                    AlgoAreConcyclic aac = new AlgoAreConcyclic(cons, p0.getGeoPoint(),
                            p1.getGeoPoint(), p2.getGeoPoint(), p3.getGeoPoint());
                    if (aac.getResult().getBoolean()) {
                        // Conjecture: Concyclicity
                        GeoElement root = new GeoBoolean(cons);
                        root.setParentAlgorithm(aac);
                        AlgoProveDetails ap = new AlgoProveDetails(cons, root);
                        ap.compute();
                        GeoElement[] o = ap.getOutput();
                        GeoList output = (GeoList) o[0];
                        if (output.size() > 0) {
                            GeoElement truth = output.get(0);
                            if (((GeoBoolean) truth).getBoolean()) {
                                // Theorem: Concyclicity
                                discoveryPool.addConcyclicity(p0, p1, p2, p3).setTrivial(false);
                            }
                        }
                        ap.remove();
                    }
                    aac.remove();
                }
            }

            if (p0.getGeoPoint().getKernel().isSilentMode()) {
                return;
            }

            // Third round: Draw circles from the discovery pool
            // (those that are not yet drawn):
            for (Circle c : discoveryPool.circles) {
                if (c.isTheorem()) {
                    if (c.getPoints().contains(p0)) {
                        if (!alreadyDrawn(c)) {
                            GeoPoint[] threepoints = c.getPoints3();
                            c.setGeoConic(addOutputCircle(threepoints[0], threepoints[1], threepoints[2]));
                        }
                        drawnCircles.add(c);
                    }
                }
            }
        }
    }

    /*
     * Extend the database by
     * collecting all parallelisms for a given input.
     */
    private void collectParallelisms(Point p0, boolean discover) {
        Pool discoveryPool = cons.getDiscoveryPool();

        HashSet<Point> prevPoints = new HashSet<Point>();
        for (Point ge : discoveryPool.points) {
            if (!ge.equals(p0)) {
                prevPoints.add(ge);
            }
        }

        HashSet<Line> allLines = new HashSet<>();
        allLines.addAll(discoveryPool.lines);

        // First run: Finding trivial parallelisms.
        for (Line l1 : allLines) {
            for (Point p1 : prevPoints) {
                if (!l1.getPoints().contains(p0) && !l1.getPoints().contains(p1)) {
                    // if they are not collinear
                    GeoPoint[] p23 = l1.getPoints2();
                    Line l2 = discoveryPool.getLine(p0, p1);
                    // Consider further trivial checks...

                    AlgoJoinPoints ajp1 = new AlgoJoinPoints(cons, null, p23[0], p23[1]);
                    AlgoJoinPoints ajp2 = new AlgoJoinPoints(cons, null, p0.getGeoPoint(), p1.getGeoPoint());
                    GeoLine gl1 = ajp1.getLine();
                    GeoLine gl2 = ajp2.getLine();

                    if (!discoveryPool.areParallel(l1, l2)) {
                        // Add {p0,p1,p2} to the trivial pool if they are trivially parallel:
                        checkParallelism(gl1, gl2);
                    }
                    gl1.remove();
                    gl2.remove();
                    ajp1.remove();
                    ajp2.remove();
                    if (!discoveryPool.areParallel(l1, l2)) {
                        discoveryPool.addDirection(l1);
                        discoveryPool.addDirection(l2);
                    }
                }
            }
        }

        if (discover) {
            // Second run: detect non-trivial parallelisms...
            for (Line l1 : allLines) {
                for (Point p1 : prevPoints) {
                    if (!l1.getPoints().contains(p0) && !l1.getPoints().contains(p1)) {
                        // if they are not collinear
                        GeoPoint[] p23 = l1.getPoints2();
                        Line l2 = discoveryPool.getLine(p0, p1);
                        // Consider further trivial checks...

                        if (!discoveryPool.areParallel(l1, l2)) {
                            AlgoJoinPoints ajp1 = new AlgoJoinPoints(cons, null, p23[0], p23[1]);
                            AlgoJoinPoints ajp2 = new AlgoJoinPoints(cons, null, p0.getGeoPoint(), p1.getGeoPoint());
                            GeoLine gl1 = ajp1.getLine();
                            GeoLine gl2 = ajp2.getLine();
                            if (gl1.isParallel(gl2)) {
                                AlgoAreParallel aap = new AlgoAreParallel(cons, gl1, gl2);
                                GeoElement root = new GeoBoolean(cons);
                                root.setParentAlgorithm(aap);
                                AlgoProveDetails ap = new AlgoProveDetails(cons, root);
                                ap.compute();
                                GeoElement[] o = ap.getOutput();
                                GeoList output = (GeoList) o[0];
                                if (output.size() > 0) {
                                    GeoElement truth = output.get(0);
                                    if (((GeoBoolean) truth).getBoolean()) {
                                        // Theorem: Parallelism
                                        discoveryPool.addParallelism(l1, l2).setTrivial(false);
                                    }
                                }
                                ap.remove();
                                aap.remove();
                            }
                            gl1.remove();
                            gl2.remove();
                            ajp1.remove();
                            ajp2.remove();
                        }
                    }
                }
            }

            if (p0.getGeoPoint().getKernel().isSilentMode()) {
                return;
            }

            // Third round: Draw all lines from the discovery pool
            // (those that are not yet drawn):
            for (ParallelLines pl : discoveryPool.directions) {
                if (pl.isTheorem()) {
                    boolean showIt = false;
                    HashSet<Line> linesDrawn = new HashSet<>();
                    HashSet<Line> linesToDraw = new HashSet<>();
                    for (Line l : pl.getLines()) {
                        if (l.getPoints().contains(p0)) {
                            showIt = true;
                        }
                        if (alreadyDrawn(l)) {
                            linesDrawn.add(l);
                        } else {
                            linesToDraw.add(l);
                        }
                    }
                    if (showIt) {
                        pl.setColor(addOutputLines(linesDrawn, linesToDraw));
                        drawnDirections.add(pl);
                    }
                }
            }
        }
    }

    /*
     * Extend the database by
     * collecting all equal long segments for a given input.
     */
    private void collectEqualLongSegments(Point p0, boolean discover) {
        Pool discoveryPool = cons.getDiscoveryPool();

        HashSet<Point> allPoints = new HashSet<Point>();
        for (Point ge : discoveryPool.points) {
            allPoints.add(ge);
        }

        Combinations segments = new Combinations(allPoints, 2);
        while (segments.hasNext()) {
            Set<Point> line = segments.next();
            Iterator<Point> i = line.iterator();
            Point p1 = i.next();
            Point p2 = i.next();
            discoveryPool.addSegment(p1, p2);
        }

        HashSet<Point> prevPoints = new HashSet<Point>();
        for (Point ge : discoveryPool.points) {
            if (!ge.equals(p0)) {
                prevPoints.add(ge);
            }
        }

        HashSet<Segment> allSegments = new HashSet<>();
        allSegments.addAll(discoveryPool.segments);

        // First run: Finding trivial equalities.
        for (Segment s1 : allSegments) {
            for (Point p1 : prevPoints) {
                Point p2 = s1.getStartPoint();
                Point p3 = s1.getEndPoint();
                Segment s2 = discoveryPool.getSegment(p0, p1);
                // Consider some trivial checks...

                AlgoJoinPointsSegment ajps1 = new AlgoJoinPointsSegment(cons, null, p2.getGeoPoint(), p3.getGeoPoint());
                AlgoJoinPointsSegment ajps2 = new AlgoJoinPointsSegment(cons, null, p0.getGeoPoint(), p1.getGeoPoint());
                GeoSegment gs1 = ajps1.getSegment();
                GeoSegment gs2 = ajps2.getSegment();

                if (!discoveryPool.areEqualLong(s1, s2)) {
                    checkEquality(gs1, gs2);
                }
                gs1.remove();
                gs2.remove();
                ajps1.remove();
                ajps2.remove();
                if (!discoveryPool.areEqualLong(s1, s2)) {
                    discoveryPool.addEquality(s1);
                    discoveryPool.addEquality(s2);
                }
            }
        }

        if (discover) {
            // Second run: detect non-trivial equalities...
            for (Segment s1 : allSegments) {
                for (Point p1 : prevPoints) {
                    Point p2 = s1.getStartPoint();
                    Point p3 = s1.getEndPoint();
                    Segment s2 = discoveryPool.getSegment(p0, p1);

                    if (!discoveryPool.areEqualLong(s1, s2)) {
                        AlgoJoinPointsSegment ajps1 = new AlgoJoinPointsSegment(cons, null, p2.getGeoPoint(), p3.getGeoPoint());
                        AlgoJoinPointsSegment ajps2 = new AlgoJoinPointsSegment(cons, null, p0.getGeoPoint(), p1.getGeoPoint());
                        GeoSegment gs1 = ajps1.getSegment();
                        GeoSegment gs2 = ajps2.getSegment();
                        if (gs1.isCongruent(gs2).boolVal()) {
                            AlgoAreCongruent aac = new AlgoAreCongruent(cons, gs1, gs2);
                            GeoElement root = new GeoBoolean(cons);
                            root.setParentAlgorithm(aac);
                            AlgoProveDetails ap = new AlgoProveDetails(cons, root);
                            ap.compute();
                            GeoElement[] o = ap.getOutput();
                            GeoList output = (GeoList) o[0];
                            if (output.size() > 0) {
                                GeoElement truth = output.get(0);
                                if (((GeoBoolean) truth).getBoolean()) {
                                    // Theorem: Congruence
                                    discoveryPool.addEquality(s1, s2).setTrivial(false);
                                    }
                                }
                            ap.remove();
                            aac.remove();
                            }
                        gs1.remove();
                        gs2.remove();
                        ajps1.remove();
                        ajps2.remove();
                        }
                    }
                }
            }

            if (p0.getGeoPoint().getKernel().isSilentMode()) {
                return;
            }

            // Third round: Draw all lines from the discovery pool
            // (those that are not yet drawn):
            for (EqualLongSegments els : discoveryPool.equalLongSegments) {
                if (els.isTheorem()) {
                    boolean showIt = false;
                    HashSet<Segment> segmentsDrawn = new HashSet<>();
                    HashSet<Segment> segmentsToDraw = new HashSet<>();
                    for (Segment s : els.getSegments()) {
                        if (s.getStartPoint().equals(p0) || s.getEndPoint().equals(p0)) {
                            showIt = true;
                        }
                        if (alreadyDrawn(s)) {
                            segmentsDrawn.add(s);
                        } else {
                            segmentsToDraw.add(s);
                        }
                    }
                    if (showIt) {
                        els.setColor(addOutputSegments(segmentsDrawn, segmentsToDraw));
                        drawnSegments.add(els);
                    }
                }
            }
        }


    private boolean are3Collinear(Point pA, Point pB, Point pC, Point pD) {
        GeoPoint A, B, C, D;
        A = pA.getGeoPoint();
        B = pB.getGeoPoint();
        C = pC.getGeoPoint();
        D = pD.getGeoPoint();
        if (GeoPoint.collinear(A, B, C) || GeoPoint.collinear(A, B, D) || GeoPoint.collinear(A, C, D)
                || GeoPoint.collinear(B, C, D)) {
            return true;
        }
        return false;
    }

    private void detectOrthogonalCollinearities() {
        Pool discoveryPool = cons.getDiscoveryPool();
        for (GeoElement ortholine : cons.getGeoSetLabelOrder()) {
            if (ortholine instanceof GeoLine && ortholine.getParentAlgorithm() instanceof AlgoOrthoLinePointLine) {
                GeoPoint startpoint = ((GeoLine) ortholine).getStartPoint();
                HashSet<GeoPoint> ortholinepoints = new HashSet<>();
                GeoPoint secondpoint = null;
                // ortholinepoints.add(startpoint); // it is always there, no point to store it and waste memory
                for (GeoElement point : cons.getGeoSetLabelOrder()) {
                    if (point instanceof GeoPoint) {
                        AlgoElement ae = point.getParentAlgorithm();
                        if (ae instanceof AlgoIntersectLines) {
                            GeoLine line1 = (GeoLine) ae.getInput(0);
                            GeoLine line2 = (GeoLine) ae.getInput(1);
                            if (line1.equals(ortholine) || line2.equals(ortholine)) {
                                if (secondpoint == null) {
                                    secondpoint = (GeoPoint) point;
                                } else {
                                    ortholinepoints.add((GeoPoint) point);
                                }
                            }
                        }
                    }
                }
                if (ortholinepoints.size() > 0) {
                    discoveryPool.addLine(discoveryPool.getPoint(startpoint), discoveryPool.getPoint(secondpoint));
                    for (GeoPoint p : ortholinepoints) {
                        discoveryPool.addCollinearity(discoveryPool.getPoint(startpoint), discoveryPool.getPoint(secondpoint),
                                discoveryPool.getPoint(p)).setTrivial(true);
                    }
                }
            }
        }
    }

    public final void initialCompute() {
        if (!(this.input instanceof GeoPoint)) {
            return; // not yet implemented
        }
        Pool discoveryPool = cons.getDiscoveryPool();
        Log.debug("The discovery pool contains " +
                discoveryPool.points.size() + " points, " +
                discoveryPool.lines.size() + " lines, " +
                discoveryPool.circles.size() + " circles, " +
                discoveryPool.directions.size() + " directions and " +
                discoveryPool.equalLongSegments.size() + " segments.");

        RelationPane tablePane = cons.getApplication().getFactory().newRelationPane();
        final RelationPane.RelationRow[] rr = new RelationPane.RelationRow[1];
        rr[0] = new RelationPane.RelationRow();
        StringBuilder html = new StringBuilder("<html>");
        rr[0].setInfo(html.toString());
        Localization loc = input.getConstruction().getApplication().getLocalization();
        if (!cons.getKernel().isSilentMode()) {
            String discoveredTheoremsOnPointA = loc.getPlainDefault("DiscoveredTheoremsOnPointA",
                    "Discovered thereoms on point %0", input.getLabelSimple());
            tablePane.showDialog(discoveredTheoremsOnPointA, rr,
                    cons.getApplication());
            // FIXME: This is not shown on the web.
        }
        if (!detectProperties((GeoPoint) this.input))  {
            String msg1 = loc.getMenuDefault("UnsupportedSteps", "The construction contains unsupported steps.");
            String msg2 = loc.getMenuDefault("RedrawDifferently","Please redraw the figure in a different way.");
            tablePane.changeRowLeftColumn(0, "<html>" + msg1 + "<br>" +
                    msg2 + "</html>");
            return;
            }

        if (cons.getKernel().isSilentMode()) {
            return;
        }

        String liStyle = "class=\"RelationTool\"";

        int pointitems = 0;
        int items = 0;
        // Points
        StringBuilder points = new StringBuilder();
        for (Point p : discoveryPool.points) {
            if (p.getPoints().size() > 1) {
                points.append(p.toString());
                points.append(", ");
                pointitems = 1;
                }
            }
        if (pointitems > 0) {
            points.deleteCharAt(points.length() - 1);
            points.deleteCharAt(points.length() - 1);
        }
        points = new StringBuilder(loc.getPlainDefault("IdenticalPointsA",
                "Identical points: %0", points.toString()));

        // Lines
        StringBuilder lines = new StringBuilder();
        if (!drawnLines.isEmpty()) {
            for (Line l : drawnLines) {
                GeoLine gl = l.getGeoLine();
                if (gl != null) {
                    String color = StringUtil.toHexString((gl.getLabelColor()));
                    lines.append("<font color=\"" + color + "\">" + l.toString() + "</font>");
                } else {
                    lines.append(l.toString());
                }
                lines.append(", ");
            }
            lines.deleteCharAt(lines.length() - 1);
            lines.deleteCharAt(lines.length() - 1);
            items++;
        }
        lines = new StringBuilder(loc.getPlainDefault("CollinearPointsA",
                "Collinear points: %0", lines.toString()));

        // Circles
        StringBuilder circles = new StringBuilder();
        if (!drawnCircles.isEmpty()) {
            for (Circle c : drawnCircles) {
                GeoConic gc = c.getGeoConic();
                if (gc != null) {
                    String color = StringUtil.toHexString((gc.getLabelColor()));
                    circles.append("<font color=\"" + color + "\">" + c.toString() + "</font>");
                } else {
                    circles.append(c.toString());
                }
                circles.append(", ");
            }
            circles.deleteCharAt(circles.length() - 1);
            circles.deleteCharAt(circles.length() - 1);
            items++;
        }
        circles = new StringBuilder(loc.getPlainDefault("ConcyclicPointsA" ,
                "Concyclic points: %0", circles.toString()));

        // Parallel lines
        StringBuilder directions = new StringBuilder();
        if (!drawnDirections.isEmpty()) {
            directions.append("<ul>");
            for (ParallelLines pl : drawnDirections) {
                GColor c = pl.getColor();
                directions.append("<li " + liStyle + ">");
                if (c != null) {
                    String color = StringUtil.toHexString(c);
                    directions.append("<font color=\"" + color + "\">" + pl.toString() + "</font>");
                } else {
                    directions.append(pl.toString());
                }
            }
            directions.append("</ul>");
            items++;
        }
        directions = new StringBuilder(loc.getPlainDefault("SetsOfParallelLinesA",
                "Sets of parallel lines: %0", directions.toString()));

        // Equal long segments
        StringBuilder equalLongSegments = new StringBuilder();
        if (!drawnSegments.isEmpty()) {
            equalLongSegments.append("<ul>");
            for (EqualLongSegments els : drawnSegments) {
                GColor c = els.getColor();
                equalLongSegments.append("<li " + liStyle + ">");
                if (c != null) {
                    String color = StringUtil.toHexString(c);
                    equalLongSegments.append("<font color=\"" + color + "\">" + els.toString() + "</font>");
                } else {
                    equalLongSegments.append(els.toString());
                }
            }
            equalLongSegments.append("</ul>");
            items++;
        }
        equalLongSegments = new StringBuilder(loc.getPlainDefault("CongruentSegmentsA",
                "Congruent segments: %0", equalLongSegments.toString()));

        html = new StringBuilder("<html>");

        items = 0;
        if (pointitems > 0) {
            html.append(points);
            items++;
        }

        if (!drawnLines.isEmpty()) {
            if (items > 0) {
                html.append("<p><p>");
            }
            html.append(lines);
            items++;
        }
        if (!drawnCircles.isEmpty()) {
            if (items > 0) {
                html.append("<p><p>");
            }
            html.append(circles);
            items++;
        }
        if (!drawnDirections.isEmpty()) {
            if (items > 0) {
                html.append("<p><p>");
            }
            html.append(directions);
            items++;
        }
        if (!drawnSegments.isEmpty()) {
            if (items > 0) {
                html.append("<p><p>");
            }
            html.append(equalLongSegments);
            items++;
        }

        if (items == 0) {
            html.append("No discovered theorems were found.");
        }

        html.append("</html>");

        rr[0] = new RelationPane.RelationRow();
        rr[0].setInfo(html.toString());

        // Unsure if this helps anything. Simply copied from Relation:
        cons.getApplication().dispatchEvent(
                new Event(EventType.RELATION_TOOL, null, rr[0].getInfo()));

        tablePane.changeRowLeftColumn(0, html.toString());
    }

    private GColor nextColor(GeoElement e) {
        return e.getAutoColorScheme()
                .getNext(true);
    }

    GColor addOutputLines(HashSet<Line> drawn, HashSet<Line> toDraw) {
        ArrayList<GeoLine> ret = new ArrayList<>();
        GColor color = null;
        if (!drawn.isEmpty()) {
            Iterator<Line> it = drawn.iterator();
            Line l1 = it.next();
            GeoLine gl1 = getAlreadyDrawn(l1);
            color = gl1.getAlgebraColor();
        } else {
            if (!cons.getKernel().isSilentMode()) {
                Iterator<Line> it = toDraw.iterator();
                Line l1 = it.next();
                GeoPoint p = l1.getPoints2()[0];
                color = nextColor((GeoElement) p);
            }
        }
        boolean oldMacroMode = cons.isSuppressLabelsActive();
        HashSet<Line> allLines = new HashSet<>();
        allLines.addAll(drawn);
        allLines.addAll(toDraw);
        for (Line l : allLines) {
            GeoLine gl;
            if (drawn.contains(l)) {
                gl = getAlreadyDrawn(l);
            } else {
                GeoPoint[] ps = l.getPoints2();
                AlgoJoinPoints ajp = new AlgoJoinPoints(cons, null, ps[0], ps[1]);
                gl = ajp.getLine();
            }
            if (color != null) {
                gl.setObjColor(color);
            }
            gl.setEuclidianVisible(true);
            gl.setLineType(EuclidianStyleConstants.LINE_TYPE_FULL);
            gl.setLineThickness(2);
            gl.setLabelVisible(false);
            gl.setEuclidianVisible(true);
            gl.setLineType(EuclidianStyleConstants.LINE_TYPE_FULL);
            gl.updateVisualStyle(GProperty.COMBINED);
            cons.setSuppressLabelCreation(oldMacroMode);
            ret.add(gl);
        }
        return color;
    }

    GColor addOutputSegments(HashSet<Segment> drawn, HashSet<Segment> toDraw) {
        ArrayList<GeoSegment> ret = new ArrayList<>();
        GColor color = null;
        if (!drawn.isEmpty()) {
            Iterator<Segment> it = drawn.iterator();
            Segment s1 = it.next();
            GeoSegment gs1 = getAlreadyDrawn(s1);
            color = gs1.getAlgebraColor();
        } else {
            if (!cons.getKernel().isSilentMode()) {
                Iterator<Segment> it = toDraw.iterator();
                Segment s1 = it.next();
                GeoPoint p = s1.getStartPoint().getGeoPoint();
                color = nextColor((GeoElement) p);
            }
        }
        boolean oldMacroMode = cons.isSuppressLabelsActive();
        HashSet<Segment> allSegments = new HashSet<>();
        allSegments.addAll(drawn);
        allSegments.addAll(toDraw);
        for (Segment s : allSegments) {
            GeoSegment gs;
            if (drawn.contains(s)) {
                gs = getAlreadyDrawn(s);
            } else {
                GeoPoint ps1 = s.getStartPoint().getGeoPoint();
                GeoPoint ps2 = s.getEndPoint().getGeoPoint();
                AlgoJoinPointsSegment ajps = new AlgoJoinPointsSegment(cons, null, ps1, ps2);
                gs = ajps.getSegment();
            }
            if (color != null) {
                gs.setObjColor(color);
            }
            gs.setEuclidianVisible(true);
            gs.setLineType(EuclidianStyleConstants.LINE_TYPE_FULL);
            gs.setLineThickness(3);
            gs.setLabelVisible(false);
            gs.setEuclidianVisible(true);
            gs.setLineType(EuclidianStyleConstants.LINE_TYPE_FULL);
            gs.updateVisualStyle(GProperty.COMBINED);
            cons.setSuppressLabelCreation(oldMacroMode);
            ret.add(gs);
        }
        return color;
    }


    GeoLine addOutputLine(GeoPoint A, GeoPoint B) {
        boolean oldMacroMode = cons.isSuppressLabelsActive();
        AlgoJoinPoints ajp = new AlgoJoinPoints(cons, null, A, B);
        GeoLine l = ajp.getLine();
        if (!A.getKernel().isSilentMode()) {
            l.setObjColor(nextColor(l));
        }
        l.setEuclidianVisible(true);
        l.setLineType(EuclidianStyleConstants.LINE_TYPE_DASHED_LONG);
        l.setLabelVisible(false);
        l.updateVisualStyle(GProperty.COMBINED); // visibility and style
        cons.setSuppressLabelCreation(oldMacroMode);
        return l;
    }

    GeoConic addOutputCircle(GeoPoint A, GeoPoint B, GeoPoint C) {
        boolean oldMacroMode = cons.isSuppressLabelsActive();
        AlgoCircleThreePoints actp = new AlgoCircleThreePoints(cons, null, A, B, C);
        GeoConic circle = (GeoConic) actp.getCircle();
        if (!A.getKernel().isSilentMode()) {
            circle.setObjColor(nextColor(circle));
        }
        circle.setEuclidianVisible(true);
        circle.setLineType(EuclidianStyleConstants.LINE_TYPE_DASHED_LONG);
        circle.setLabelVisible(false);
        circle.updateVisualStyle(GProperty.COMBINED); // visibility and style
        cons.setSuppressLabelCreation(oldMacroMode);
        return circle;
    }

    private boolean alreadyDrawn(Line l) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoLine && !(ge instanceof GeoSegment)) {
                GeoPoint p1 = ((GeoLine) ge).startPoint;
                GeoPoint p2 = ((GeoLine) ge).endPoint;
                Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                HashSet<Point> points = l.getPoints();
                if (points.contains(pp1) && points.contains(pp2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private GeoLine getAlreadyDrawn(Line l) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoLine && !(ge instanceof GeoSegment)) {
                GeoPoint p1 = ((GeoLine) ge).startPoint;
                GeoPoint p2 = ((GeoLine) ge).endPoint;
                HashSet<Point> points = l.getPoints();
                Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                if (points.contains(pp1) && points.contains(pp2)) {
                    return (GeoLine) ge;
                }
            }
        }
        return null;
    }

    private GeoSegment getAlreadyDrawn(Segment s) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoSegment) {
                GeoPoint p1 = ((GeoSegment) ge).startPoint;
                GeoPoint p2 = ((GeoSegment) ge).endPoint;
                Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                Point pq1 = s.getStartPoint();
                Point pq2 = s.getEndPoint();
                if ((pq1.equals(pp1) && pq2.equals(pp2)) ||
                        (pq1.equals(pp2) && pq2.equals(pp1))) {
                    return (GeoSegment) ge;
                }
            }
        }
        return null;
    }

    private boolean alreadyDrawn(Circle c) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoConic && ((GeoConic) ge).isCircle()) {
                ArrayList<GeoPointND> cpoints = ((GeoConic) ge).getPointsOnConic();
                if (cpoints.size() == 3) {
                    GeoPoint p1 = (GeoPoint) cpoints.get(0);
                    GeoPoint p2 = (GeoPoint) cpoints.get(1);
                    GeoPoint p3 = (GeoPoint) cpoints.get(2);
                    Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                    Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                    Point pp3 = cons.getDiscoveryPool().getPoint(p3);
                    HashSet<Point> points = c.getPoints();
                    if (points.contains(pp1) && points.contains(pp2) && points.contains(pp3)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // yet unused
    private GeoConic getAlreadyDrawn(Circle c) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoConic && ((GeoConic) ge).isCircle()) {
                ArrayList<GeoPointND> cpoints = ((GeoConic) ge).getPointsOnConic();
                if (cpoints.size() == 3) {
                    GeoPoint p1 = (GeoPoint) cpoints.get(0);
                    GeoPoint p2 = (GeoPoint) cpoints.get(1);
                    GeoPoint p3 = (GeoPoint) cpoints.get(2);
                    Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                    Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                    Point pp3 = cons.getDiscoveryPool().getPoint(p3);
                    HashSet<Point> points = c.getPoints();
                    if (points.contains(pp1) && points.contains(pp2) && points.contains(pp3)) {
                        return (GeoConic) ge;
                    }
                }
            }
        }
        return null;
    }

    private boolean alreadyDrawn(Segment s) {
        for (GeoElement ge : cons.getGeoSetLabelOrder()) {
            if (ge instanceof GeoSegment) {
                GeoPoint p1 = ((GeoLine) ge).startPoint;
                GeoPoint p2 = ((GeoLine) ge).endPoint;
                Point pp1 = cons.getDiscoveryPool().getPoint(p1);
                Point pp2 = cons.getDiscoveryPool().getPoint(p2);
                Point pq1 = s.getStartPoint();
                Point pq2 = s.getEndPoint();
                if ((pq1.equals(pp1) && pq2.equals(pp2)) ||
                        (pq1.equals(pp2) && pq2.equals(pp1))) {
                    return true;
                }
            }
        }
        return false;
    }

    void checkParallelism(GeoLine l1, GeoLine l2) {
        // TODO. To be written.
    }

    void checkEquality(GeoSegment ss1, GeoSegment ss2) {
        // TODO. To be written.
    }

    void checkIdenticality(GeoPoint p1, GeoPoint p2) {
        // TODO. To be written.
    }

    void checkConcyclicity(Point pA, Point pB, Point pC, Point pD) {
        GeoPoint A, B, C, D;
        A = pA.getGeoPoint();
        B = pB.getGeoPoint();
        C = pC.getGeoPoint();
        D = pD.getGeoPoint();
        /*
         * TODO. This is certainly incomplete.
         */
        Pool discoveryPool = cons.getDiscoveryPool();

        AlgoElement ae = D.getParentAlgorithm();

        if (ae instanceof AlgoIntersectSingle) {
            AlgoElement ae2 = ((AlgoIntersectSingle) ae).getAlgo();
            if (ae2 instanceof AlgoIntersectLineConic) {
                GeoConic c = ((AlgoIntersectLineConic) ae2).getConic();
                AlgoElement ae3 = c.getParentAlgorithm();
                if (c.isCircle() && ae3 instanceof AlgoCircleThreePoints) {
                    GeoPoint j1 = ((AlgoCircleThreePoints) ae3).getA();
                    GeoPoint j2 = ((AlgoCircleThreePoints) ae3).getB();
                    GeoPoint j3 = ((AlgoCircleThreePoints) ae3).getC();
                    if (j1 != null && j2 != null && j3 != null) {
                        // TODO. This is ugly, consider writing more beautiful code here
                        // (all permutations of A, B and C are required:
                        if ((j1.equals(A) && j2.equals(B) && j3.equals(C)) ||
                                (j1.equals(A) && j2.equals(C) && j3.equals(B)) ||
                                (j1.equals(B) && j2.equals(A) && j3.equals(C)) ||
                                (j1.equals(B) && j2.equals(C) && j3.equals(A)) ||
                                (j1.equals(C) && j2.equals(B) && j3.equals(A)) ||
                                (j1.equals(C) && j2.equals(A) && j3.equals(B))) {
                            // D is an intersection of circle ABC and something:
                            discoveryPool.addConcyclicity(pA, pB, pC, pD).setTrivial(true);
                        }
                    }
                }
            }
        }
    }

    void checkCollinearity(Point pA, Point pB, Point pC) {
        GeoPoint A, B, C;
        A = pA.getGeoPoint();
        B = pB.getGeoPoint();
        C = pC.getGeoPoint();
        /*
         * TODO. This is certainly incomplete.
         */
        Pool discoveryPool = cons.getDiscoveryPool();

        AlgoElement ae = C.getParentAlgorithm();

        if (ae instanceof AlgoIntersectLines) {
            GeoElement[] inps = ((AlgoIntersectLines) ae).getInput();
            GeoPoint i1 = ((GeoLine) inps[0]).getStartPoint();
            GeoPoint i2 = ((GeoLine) inps[0]).getEndPoint();
            GeoPoint j1 = ((GeoLine) inps[1]).getStartPoint();
            GeoPoint j2 = ((GeoLine) inps[1]).getEndPoint();

            if ((i1 != null && i2 != null && ((i1.equals(A) && i2.equals(B)) ||
                    (i1.equals(B) && i2.equals(A)))) ||
                    (j1 != null && j2 != null && ((j1.equals(A) && j2.equals(B)) ||
                            (j1.equals(B) && j2.equals(A))))
            ) {
                // C is an intersection of AB and something:
                discoveryPool.addCollinearity(pA, pB, pC).setTrivial(true);
            }
        }
        if (ae instanceof AlgoMidpoint) {
            GeoElement[] inps = ((AlgoMidpoint) ae).getInput();
            if ((inps[0].equals(A) && inps[1].equals(B)) ||
                    (inps[0].equals(B) && inps[1].equals(A))) {
                // C is a midpoint of AB:
                discoveryPool.addCollinearity(pA, pB, pC).setTrivial(true);
            }
        }
        if (ae instanceof AlgoMidpointSegment) {
            GeoSegment seg = (GeoSegment) ((AlgoMidpointSegment) ae).getInput(0);
            GeoPoint p1 = seg.startPoint;
            GeoPoint p2 = seg.endPoint;
            if ((p1.equals(A) && p2.equals(B)) ||
                    (p1.equals(B) && p2.equals(A))) {
                // C is a midpoint of AB:
                discoveryPool.addCollinearity(pA, pB, pC).setTrivial(true);
            }
        }
        if (ae instanceof AlgoPointOnPath) {
            Path p = ((AlgoPointOnPath) ae).getPath();
            AlgoElement aep = p.getParentAlgorithm();
            if (aep instanceof AlgoJoinPointsSegment) {
                AlgoJoinPointsSegment ajps = (AlgoJoinPointsSegment) aep;
                GeoElement[] ges = ajps.getInput();
                if ((ges[0].equals(A) && ges[1].equals(B)) ||
                        ges[0].equals(B) && ges[1].equals(A)) {
                    // C is defined to be on segment AB
                    discoveryPool.addCollinearity(pA, pB, pC).setTrivial(true);
                }
            }
        }
    }
}