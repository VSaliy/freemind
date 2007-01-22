/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2007  Joerg Mueller, Daniel Polansky, Dimitri Polivaev, Christian Foltin and others.
 *
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Created on 10.01.2007
 */
/*$Id: ScriptEditorPanel.java,v 1.1.2.3 2007-01-22 21:58:30 christianfoltin Exp $*/
package plugins.script;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import freemind.controller.BlindIcon;
import freemind.controller.StructuredMenuHolder;
import freemind.controller.actions.generated.instance.ScriptEditorWindowConfigurationStorage;
import freemind.main.FreeMindMain;
import freemind.main.Tools;
import groovy.lang.GroovyRuntimeException;

/**
 * @author foltin
 * 
 */
public class ScriptEditorPanel extends JDialog {
	private static final String WINDOW_PREFERENCE_STORAGE_PROPERTY = "plugins.script.ScriptEditorPanel/window_positions";

	private final FreeMindMain mFrame;

	private final ScriptModel mScriptModel;

	private JList mScriptList;

	private JTextArea mScriptTextField;

	private DefaultListModel mListModel;

	private Integer mLastSelected = null;

	private JTextArea mScriptResultField;

	private JSplitPane mCentralUpperPanel;

	private JSplitPane mCentralPanel;

	private JSplitPane mCentralLowerPanel;

	private JTextArea mScriptOutputField;

	private final class RunAction extends AbstractAction {
		private RunAction(String pArg0) {
			super(pArg0);
		}

		public void actionPerformed(ActionEvent arg0) {
			// do something
			storeCurrent();
			if (!mScriptList.isSelectionEmpty()) {
				mScriptOutputField.removeAll();
				String resultString = "";
				try {
					resultString = mScriptModel.executeScript(mScriptList
							.getSelectedIndex(), new PrintStream(new OutputStream(){

								public void write(int pByte) throws IOException {
									mScriptOutputField.append(new String(new byte[]{(byte) pByte}));
								}}));
				} catch (GroovyRuntimeException e) {
					// freemind.main.Resources.getInstance().logExecption(e);
					// ByteArrayOutputStream byteArrayOutputStream = new
					// ByteArrayOutputStream();
					// PrintStream writer = new
					// PrintStream(byteArrayOutputStream);
					// e.printStackTrace(writer);
					// resultString = byteArrayOutputStream.toString();
					resultString = e.getMessage();
				}
				mScriptResultField.removeAll();
				mScriptResultField.setText(resultString);
			}
		}
	}

	public static class ScriptHolder {
		String mScript;

		String mScriptName;

		/**
		 * @param pScriptName
		 *            script name (starting with "script")
		 * @param pScript
		 *            script content
		 */
		public ScriptHolder(String pScriptName, String pScript) {
			super();
			mScript = pScript;
			mScriptName = pScriptName;
		}

		public String getScript() {
			return mScript;
		}

		public String getScriptName() {
			return mScriptName;
		}

		public ScriptHolder setScript(String pScript) {
			mScript = pScript;
			return this;
		}

		public ScriptHolder setScriptName(String pScriptName) {
			mScriptName = pScriptName;
			return this;
		}
	}

	public interface ScriptModel {
		int getAmountOfScripts();

		/**
		 * @param pIndex
		 *            zero-based
		 * @return a script
		 */
		ScriptHolder getScript(int pIndex);

		void setScript(int pIndex, ScriptHolder pScript);

		String executeScript(int pIndex, PrintStream outStream);

		void storeDialogPositions(ScriptEditorPanel pPanel,
				ScriptEditorWindowConfigurationStorage pStorage,
				String pWindow_preference_storage_property);

		ScriptEditorWindowConfigurationStorage decorateDialog(
				ScriptEditorPanel pPanel,
				String pWindow_preference_storage_property);
	}

	public ScriptEditorPanel(ScriptModel pScriptModel, FreeMindMain pFrame) {
		super(pFrame.getJFrame(), true /* modal */);
		mScriptModel = pScriptModel;
		mFrame = pFrame;
		// build the panel:
		this.setTitle(pFrame
				.getResourceString("plugins/ScriptEditor/window.title"));
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				disposeDialog();
			}
		});
		Tools.addEscapeActionToDialog(this, new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				disposeDialog();
			}
		});
		Container contentPane = this.getContentPane();

		contentPane.setLayout(new BorderLayout());
		mListModel = new DefaultListModel();
		mScriptList = new JList(mListModel);
		mScriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		mScriptList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent pEvent) {
				System.out.println("List selection:" + pEvent);
				if (pEvent.getValueIsAdjusting())
					return;
				select(mScriptList.getSelectedIndex());
			}
		});
		// add(mScriptList, BorderLayout.WEST);
		mScriptTextField = new JTextArea();
		mScriptTextField.setTabSize(2);
		mCentralUpperPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				mScriptList, new JScrollPane(mScriptTextField));
		mCentralUpperPanel.setContinuousLayout(true);
		mScriptResultField = new JTextArea();
		mScriptResultField.setEditable(false);
		mScriptResultField.setWrapStyleWord(true);
		mScriptOutputField = new JTextArea();
		mScriptOutputField.setEditable(false);
		mScriptOutputField.setWrapStyleWord(true);
		mCentralLowerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(mScriptOutputField), new JScrollPane(mScriptResultField));
		mCentralLowerPanel.setContinuousLayout(true);
		mCentralPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				mCentralUpperPanel, mCentralLowerPanel);
		mCentralPanel.setDividerLocation(0.8);
		mCentralPanel.setContinuousLayout(true);
		contentPane.add(mCentralPanel, BorderLayout.CENTER);
		updateFields();
		mScriptTextField.repaint();
		// menu:
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu(pFrame
				.getResourceString("plugins/ScriptEditor.menu_actions"));
		AbstractAction runAction = new RunAction(pFrame
				.getResourceString("plugins/ScriptEditor.run"));

		AbstractAction[] actionList = new AbstractAction[] { runAction };
		for (int i = 0; i < actionList.length; i++) {
			AbstractAction action = actionList[i];
			JMenuItem item = menu.add(action);
			item.setIcon(new BlindIcon(StructuredMenuHolder.ICON_SIZE));
		}
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		// Retrieve window size and column positions.
		ScriptEditorWindowConfigurationStorage storage = mScriptModel
				.decorateDialog(this, WINDOW_PREFERENCE_STORAGE_PROPERTY);
		if (storage != null) {
			mCentralUpperPanel.setDividerLocation(storage.getLeftRatio());
			mCentralPanel.setDividerLocation(storage.getTopRatio());
		}

	}

	private void updateFields() {
		mScriptList.removeAll();
		for (int i = 0; i < mScriptModel.getAmountOfScripts(); ++i) {
			ScriptHolder script = mScriptModel.getScript(i);
			mListModel.addElement(script.getScriptName());
		}
	}

	private void select(int pIndex) {
		storeCurrent();
		// set new script
		mScriptTextField.setText(mScriptModel.getScript(pIndex).getScript());
		// set last one:
		mLastSelected = new Integer(pIndex);
	}

	private void storeCurrent() {
		if (mLastSelected != null) {
			// store old value:
			int oldIndex = mLastSelected.intValue();
			mScriptModel.setScript(oldIndex, mScriptModel.getScript(oldIndex)
					.setScript(mScriptTextField.getText()));
		}
	}

	/**
	 * 
	 */
	private void disposeDialog() {
		// store current script:
		if (!mScriptList.isSelectionEmpty()) {
			select(mScriptList.getSelectedIndex());
		}
		// store window positions:
		ScriptEditorWindowConfigurationStorage storage = new ScriptEditorWindowConfigurationStorage();
		storage.setLeftRatio(mCentralUpperPanel.getDividerLocation());
		storage.setTopRatio(mCentralPanel.getDividerLocation());
		mScriptModel.storeDialogPositions(this, storage,
				WINDOW_PREFERENCE_STORAGE_PROPERTY);
		this.setVisible(false);
		this.dispose();
	}

}
