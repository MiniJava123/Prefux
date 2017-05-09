package fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import prefux.Constants;
import prefux.FxDisplay;
import prefux.Visualization;
import prefux.action.ActionList;
import prefux.action.GroupAction;
import prefux.action.ItemAction;
import prefux.action.RepaintAction;
import prefux.action.animate.ColorAnimator;
import prefux.action.animate.PolarLocationAnimator;
import prefux.action.animate.VisibilityAnimator;
import prefux.action.assignment.ColorAction;
import prefux.action.assignment.FontAction;
import prefux.action.layout.CollapsedSubtreeLayout;
import prefux.action.layout.graph.RadialTreeLayout;
import prefux.activity.SlowInSlowOutPacer;
import prefux.controls.DragControl;
import prefux.controls.ZoomToFitControl;
import prefux.data.Graph;
import prefux.data.Node;
import prefux.data.Tuple;
import prefux.data.event.TupleSetListener;
import prefux.data.io.GraphMLReader;
import prefux.data.tuple.DefaultTupleSet;
import prefux.data.tuple.TupleSet;
import prefux.render.AbstractShapeRenderer;
import prefux.render.DefaultRendererFactory;
import prefux.render.EdgeRenderer;
import prefux.render.LabelRenderer;
import prefux.util.ColorLib;
import prefux.util.FontLib;
import prefux.util.ui.SearchPane;
import prefux.visual.VisualItem;
import prefux.visual.expression.InGroupPredicate;

import java.util.Iterator;

public class RadialGraphView extends Application {
    public static final String DATA_FILE = "socialnet.xml";
    private static final double WIDTH = 900;
    private static final double HEIGHT = 750;
    private static final String GROUP = "graph";
    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    private static final String linear = "linear";
    private static Visualization vis;
    private LabelRenderer nodeRenderer;
    private EdgeRenderer edgeRenderer;
    private String label = "label";
    private Graph graph;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("p r e f u x  |  r a d i a l g r a p h v i e w");
        BorderPane root = new BorderPane();
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        root.getStyleClass().add("display");
        primaryStage.show();

        // -- set up visualization --
        graph = new GraphMLReader().readGraph(DATA_FILE);
        vis = new Visualization();

        vis.add(tree, this.graph);
        vis.setInteractive(treeEdges, null, false);

        // -- set up renderers --
        nodeRenderer = new LabelRenderer(label);
        nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        nodeRenderer.setHorizontalAlignment(Constants.CENTER);
        edgeRenderer = new EdgeRenderer();

        DefaultRendererFactory rf = new DefaultRendererFactory(nodeRenderer);
        rf.add(new InGroupPredicate(treeEdges), edgeRenderer);
        vis.setRendererFactory(rf);

        // colors
        ItemAction nodeColor = new NodeColorAction(treeNodes);
        ItemAction textColor = new TextColorAction(treeNodes);
        vis.putAction("textColor", textColor);

        ItemAction edgeColor = new ColorAction(treeEdges, VisualItem.STROKECOLOR, ColorLib.rgb(200, 200, 200));

        FontAction fonts = new FontAction(treeNodes, FontLib.getFont("Tahoma", 10));
        fonts.add("ingroup('_focus_')", FontLib.getFont("Tahoma", 11));

        // recolor
        ActionList recolor = new ActionList();
        recolor.add(nodeColor);
        recolor.add(textColor);
        vis.putAction("recolor", recolor);

        // repaint
        ActionList repaint = new ActionList();
        repaint.add(recolor);
        repaint.add(new RepaintAction());
        vis.putAction("repaint", repaint);

        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        vis.putAction("animatePaint", animatePaint);

        // create the tree layout action
        RadialTreeLayout treeLayout = new RadialTreeLayout(tree);
        //treeLayout.setAngularBounds(-Math.PI/2, Math.PI);
        vis.putAction("treeLayout", treeLayout);

        CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree);
        vis.putAction("subLayout", subLayout);

        // create the filtering and layout
        ActionList filter = new ActionList();
        filter.add(new TreeRootAction(tree));
        filter.add(fonts);
        filter.add(treeLayout);
        filter.add(subLayout);
        filter.add(textColor);
        filter.add(nodeColor);
        filter.add(edgeColor);
        vis.putAction("filter", filter);

        // animated transition
        ActionList animate = new ActionList(1250);
        animate.setPacingFunction(new SlowInSlowOutPacer());
//        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new PolarLocationAnimator(treeNodes, linear));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new RepaintAction());
        vis.putAction("animate", animate);
        vis.alwaysRunAfter("filter", "animate");

        FxDisplay display = new FxDisplay(vis);
//        display.setItemSorter(new TreeDepthItemSorter());
        display.addControlListener(new DragControl());
        display.addControlListener(new ZoomToFitControl());
//        display.addControlListener(new ZoomControl());
//        display.addControlListener(new PanControl());
//        display.addControlListener(new FocusControl(1, "filter"));
//        display.addControlListener(new HoverActionControl("repaint"));

        // ------------------------------------------------

        // filter graph and perform layout


        root.setCenter(display);
        // add the "title" label...
        root.setBottom(new SearchPane());
        vis.run("filter");

        // maintain a set of items that should be interpolated linearly
        // this isn't absolutely necessary, but makes the animations nicer
        // the PolarLocationAnimator should read this set and act accordingly
        vis.addFocusGroup(linear, new DefaultTupleSet());
        vis.getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(
                new TupleSetListener() {
                    public void tupleSetChanged(TupleSet tSet, Tuple[] add, Tuple[] rem) {
                        TupleSet linearInterp = vis.getGroup(linear);
                        if (add.length < 1) return;
                        linearInterp.clear();
                        for (Node n = (Node) add[0]; n != null; n = n.getParent())
                            linearInterp.addTuple(n);
                    }
                }
        );


    }


    // ------------------------------------------------------------------------

    /**
     * Switch the root of the tree by requesting a new spanning tree
     * at the desired root
     */
    public static class TreeRootAction extends GroupAction {
        public TreeRootAction(String graphGroup) {
            super(graphGroup);
        }

        public void run(double frac) {
            TupleSet focus = vis.getGroup(Visualization.FOCUS_ITEMS);
            if (focus == null || focus.getTupleCount() == 0) return;

            Graph g = (Graph) vis.getGroup(m_group);
            Node f = null;
            Iterator tuples = focus.tuples();
            while (tuples.hasNext() && !g.containsTuple(f = (Node) tuples.next())) {
                f = null;
            }
            if (f == null) return;
            g.getSpanningTree(f);
        }
    }

    /**
     * Set node fill colors
     */
    public static class NodeColorAction extends ColorAction {
        public NodeColorAction(String group) {
            super(group, VisualItem.FILLCOLOR, ColorLib.rgba(255, 255, 255, 0));
            add("_hover", ColorLib.gray(220, 230));
            add("ingroup('_search_')", ColorLib.rgb(255, 190, 190));
            add("ingroup('_focus_')", ColorLib.rgb(198, 229, 229));
        }

    } // end of inner class NodeColorAction

    /**
     * Set node text colors
     */
    public static class TextColorAction extends ColorAction {
        public TextColorAction(String group) {
            super(group, VisualItem.TEXTCOLOR, ColorLib.gray(0));
            add("_hover", ColorLib.rgb(255, 0, 0));
        }
    } // end of inner class TextColorAction
}
