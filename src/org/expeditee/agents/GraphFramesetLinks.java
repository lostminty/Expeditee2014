package org.expeditee.agents;

import java.awt.Color;
import java.util.List;

import org.expeditee.actions.Misc;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FrameUtils;
import org.expeditee.items.Item;
import org.expeditee.items.Line;
import org.expeditee.items.Text;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.view.mxGraph;
import java.awt.Font;

/**
 * Generates a graph/visualization of the current frameset in terms of how frames are linked to each other
 * The layout type used (tree, organic, circle, hierarchical, or the default, fastorganic) can be specified 
 * when calling the action (e.g. 'GraphFramesetLinks circle').
 * <br >
 * Uses JGraphX to compute the graph layout
 * @author ngw8
 *
 */
public class GraphFramesetLinks extends DefaultAgent {
	
	private String layoutMethod;
	
	public GraphFramesetLinks(String layoutMethod) {
		this.layoutMethod = layoutMethod;
	}
	
	public GraphFramesetLinks() {
		this.layoutMethod = "";
	}

	@Override
	protected Frame process(Frame frame) {
		String toProcessFramesetName = frame.getFramesetName();

		Frame toProccess = FrameUtils.getFrame(toProcessFramesetName + 0);

		Frame resultsFrame;
		
		String resultsFramesetName = "LinksGraphResults";

		Color vertColor = new Color(51,145,148);
		Color unlinkedVertColor = new Color(251,107,65);
		Color outgoingVertColor = new Color(246,216,107);
		Color lineColor = new Color(0,0,0, 20);

		try {
			// Creating frameset that is used to hold any generated link graphs
			FrameIO.CreateNewFrameset(resultsFramesetName);
		} catch (Exception e2) {
			// frameset already exists
		}

		resultsFrame = FrameIO.CreateFrame(resultsFramesetName, toProcessFramesetName, null);
		resultsFrame.removeAllItems(resultsFrame.getAllItems());
		
		mxGraph graph = new mxGraph();
		mxGraphModel model = (mxGraphModel) graph.getModel();
		mxGraphLayout layout;

		// Deciding which layout method to use
		if(this.layoutMethod.equalsIgnoreCase("organic")) {
			layout = new mxOrganicLayout(graph);
		} else if (this.layoutMethod.equalsIgnoreCase("circle")) {
			layout = new mxCircleLayout(graph);
		} else if (this.layoutMethod.equalsIgnoreCase("tree")) {
			layout = new mxCompactTreeLayout(graph);
			((mxCompactTreeLayout)layout).setLevelDistance(30);
		} else if(this.layoutMethod.equalsIgnoreCase("hierarchical")) {
			layout = new mxHierarchicalLayout(graph);
		} else {
			layout = new mxFastOrganicLayout(graph);
		}
		
		model.beginUpdate();

		try {
			while ((toProccess = FrameIO.LoadNext(toProccess)) != null) {

				Object sourceVert = model.getCell(toProccess.getName());

				if (sourceVert == null) {
					sourceVert = graph.insertVertex(graph.getDefaultParent(), toProccess.getName(), toProccess.getName(), 0, 0, 10, 10);
				}

				List<Item> items = toProccess.getItems();

				for (Item item : items) {
					if (item.getLink() != null) {
						
						Object destinationVert = model.getCell(item.getAbsoluteLink());

						if (destinationVert == null) {
							destinationVert = graph.insertVertex(graph.getDefaultParent(), item.getAbsoluteLink(), item.getAbsoluteLink(), 0, 0, 10, 10);
						}
						
						// Scaling the vertex based on the number of incoming links
						mxGeometry destGeo = ((mxCell)(destinationVert)).getGeometry();
						destGeo.setHeight(destGeo.getHeight() + 1);
						destGeo.setWidth(destGeo.getHeight());

						graph.insertEdge(graph.getDefaultParent(), null, null, sourceVert, destinationVert);
					}
				}
			}

			layout.execute(graph.getDefaultParent());

		} finally {
			graph.getModel().endUpdate();
		}

		Object[] verts = graph.getChildVertices(graph.getDefaultParent());

		for (Object v : verts) {
			mxGeometry geo = graph.getCellGeometry(v);

			Item circleCenter = resultsFrame.addDot((int) geo.getCenterX(),(int) geo.getCenterY());
			
			Text circleEdge = (Text) resultsFrame.addText((int) geo.getX(), (int) geo.getCenterY(), "@c", null, ((mxCell) v).getId());
			
			Line circle = new Line(circleCenter, circleEdge, resultsFrame.getNextItemID());

			circleCenter.setLink(((mxCell) v).getId());
			circleCenter.setTooltip("text: " + ((mxCell) v).getId());
			circleCenter.setTooltip("text: In: " + mxGraphModel.getDirectedEdgeCount(model, v, false));
			circleCenter.setTooltip("text: Out: " + mxGraphModel.getDirectedEdgeCount(model, v, true));
			
			circleCenter.setThickness(0);
			circleCenter.setFillColor(vertColor);

			// If the vert is not in the current frameset, style it differently to denote this
			if (!((String) model.getValue(v)).replaceAll("\\d+$", "").equals(toProcessFramesetName)) {
				circleCenter.setFillColor(outgoingVertColor);
			}
			
			// If there are no incoming edges (i.e. no frames link to the frame the vert represents), style it differently to denote this
			if (mxGraphModel.getDirectedEdgeCount(model, v, false) <= 0) {
				circleCenter.setFillColor(unlinkedVertColor);
			}
			
			resultsFrame.addItem(circle);
		}

		Object[] edges = graph.getChildEdges(graph.getDefaultParent());

		for (Object e : edges) {
			mxGeometry sourceGeo = graph.getCellGeometry(((mxCell) e).getTerminal(true));
			mxGeometry destGeo = graph.getCellGeometry(((mxCell) e).getTerminal(false));

			// create the endpoints of the edge/line
			Item lineEnd = resultsFrame.createDot();
			Item lineStart = resultsFrame.createDot();

			lineStart.setPosition((int) sourceGeo.getCenterX(), (int) sourceGeo.getCenterY());
			lineEnd.setPosition((int) destGeo.getCenterX(), (int) destGeo.getCenterY());

			lineStart.anchor();
			lineEnd.anchor();

			lineEnd.setArrowheadLength(10);
			
			// create the edge/line
			Line line = new Line(lineStart, lineEnd, resultsFrame.getNextItemID());
			
			line.setColor(lineColor);
			line.setThickness(1);
			
			// The edge/line currently has its endpoints at the vert centers, which doesn't look nice, so this shifts them out to the edge of the verts/circles
			float ratio = (float) (destGeo.getWidth() / (2 * line.getLength()));
			int x3 = Math.round(ratio* ((line.getStartItem().getX() - line.getEndItem().getX())));
			int y3 = Math.round(ratio * ((line.getStartItem().getY() - line.getEndItem().getY())));

			line.getEndItem().setX(line.getEndItem().getX() + x3);
			line.getEndItem().setY(line.getEndItem().getY() + y3);
			
			// Have to redo the calculation for the source vert, as it could have a different size
			ratio = (float) (sourceGeo.getWidth() / (2 * line.getLength()));
			x3 = Math.round(ratio* ((line.getEndItem().getX() - line.getStartItem().getX())));
			y3 = Math.round(ratio * ((line.getEndItem().getY() - line.getStartItem().getY())));

			line.getStartItem().setX(line.getStartItem().getX() + x3);
			line.getStartItem().setY(line.getStartItem().getY() + y3);

			resultsFrame.addItem(line);
		}
		
		Font keyFont = Font.decode(Font.SANS_SERIF + " 14");
		
		Text keyLink = resultsFrame.addText(0, 0, "\u2192 = Link", null);
		Text keyFrame = resultsFrame.addText(0, 0, "\u25CF = Frame", null);
		Text keyFrameUnlinked = resultsFrame.addText(0, 0, "\u25CF = Frame with no incoming links", null);
		Text keyFrameOutgoing = resultsFrame.addText(0, 0, "\u25CF = Frame in a different frameset", null);
		
		keyLink.setFont(keyFont);
		keyLink.setColor(Color.DARK_GRAY);
		
		keyFrame.setFont(keyFont);;
		keyFrame.setColor(vertColor);
		
		keyFrameUnlinked.setFont(keyFont);
		keyFrameUnlinked.setColor(unlinkedVertColor);
		
		keyFrameOutgoing.setFont(keyFont);
		keyFrameOutgoing.setColor(outgoingVertColor);
		
		keyFrameOutgoing.setAnchorBottom(10f);
		keyFrameUnlinked.setAnchorBottom(30f);
		keyFrame.setAnchorBottom(50f);
		keyLink.setAnchorBottom(70f);
		
		keyFrameOutgoing.setAnchorLeft(10f);
		keyFrameUnlinked.setAnchorLeft(10f);
		keyFrame.setAnchorLeft(10f);
		keyLink.setAnchorLeft(10f);

		// Moving to the area of the frame that contains the graph
		Misc.pan(resultsFrame, - (int) graph.getGraphBounds().getX(), - (int) graph.getGraphBounds().getY());

		FrameIO.SaveFrame(resultsFrame);

		Text link = new Text(toProcessFramesetName + " graph");
		link.setLink(resultsFrame.getName());
		link.setPosition(FrameMouseActions.getPosition());
		FrameMouseActions.pickup(link);
		return null;
	}
}
