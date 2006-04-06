/*
 * Created on 5.06.2004
 *
 */
package accessories.plugins;

import java.awt.Color;

import freemind.controller.actions.generated.instance.EditNodeAction;
import freemind.controller.actions.generated.instance.XmlAction;
import freemind.modes.MindMapNode;
import freemind.modes.NodeAdapter;
import freemind.modes.mindmapmode.actions.xml.ActionHandler;
import freemind.modes.mindmapmode.actions.xml.ActionPair;
import freemind.modes.mindmapmode.hooks.PermanentMindMapNodeHookAdapter;

/**
 * @author foltin
 *
 */
public class RevisionPlugin extends PermanentMindMapNodeHookAdapter implements ActionHandler {

	static boolean alreadyUsed = false;

	private Color color;

    /**
	 * 
	 */
	public RevisionPlugin() {
		super();
	}


	/* (non-Javadoc)
	 * @see freemind.extensions.NodeHook#invoke(freemind.modes.MindMapNode)
	 */
	public void invoke(MindMapNode node) {
		super.invoke(node);
		if(alreadyUsed == false ){
			color = Color.YELLOW;
			// new register: 
			getMindMapController().getActionFactory().registerHandler(this);
			alreadyUsed = true;
		}
	}

	public void shutdownMapHook() {
		getMindMapController().getActionFactory().deregisterHandler(this);
		super.shutdownMapHook();
	}
    /* (non-Javadoc)
     * @see freemind.controller.actions.ActionHandler#executeAction(freemind.controller.actions.ActionPair)
     */
    public void executeAction(ActionPair pair) {
    	XmlAction action = pair.getDoAction();
    	if(action instanceof EditNodeAction) {
    		// there is an edit action.
			EditNodeAction editAction = (EditNodeAction) action;
			NodeAdapter node = getMindMapController().getNodeFromID(editAction.getNode());
			node.setBackgroundColor(color);
			nodeChanged(node);
    	}
    }

    /* (non-Javadoc)
     * @see freemind.controller.actions.ActionHandler#startTransaction(java.lang.String)
     */
    public void startTransaction(String name) {
    }

    /* (non-Javadoc)
     * @see freemind.controller.actions.ActionHandler#endTransaction(java.lang.String)
     */
    public void endTransaction(String name) {
    }


}