/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.application.menu;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.horstmann.violet.application.ApplicationStopper;
import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.Workspace;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
//TP3//
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.edges.AggregationEdge;
import com.horstmann.violet.product.diagram.classes.edges.AssociationEdge;
import com.horstmann.violet.product.diagram.classes.edges.CompositionEdge;
import com.horstmann.violet.product.diagram.classes.edges.InheritanceEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
//TP3//
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;


/**
 * Represents the file menu on the editor frame
 * 
 * @author Alexandre de Pellegrin
 * 
 */
@ResourceBundleBean(resourceReference = MenuFactory.class)
public class FileMenu extends JMenu
{

	/**
	 * Default constructor
	 * 
	 * @param mainFrame
	 */
	@ResourceBundleBean(key = "file")
	public FileMenu(MainFrame mainFrame)
	{
		ResourceBundleInjector.getInjector().inject(this);
		BeanInjector.getInjector().inject(this);
		this.mainFrame = mainFrame;
		createMenu();
		addWindowsClosingListener();
	}

	/**
	 * @return 'new file' menu
	 */
	public JMenu getFileNewMenu()
	{
		return this.fileNewMenu;
	}

	/**
	 * @return recently opened file menu
	 */
	public JMenu getFileRecentMenu()
	{
		return this.fileRecentMenu;
	}

	/**
	 * Initialize the menu
	 */
	private void createMenu()
	{
		initFileNewMenu();
		initFileOpenItem();
		initFileCloseItem();
		initFileRecentMenu();
		initFileSaveItem();
		initFileSaveAsItem();
		initFileExportMenu();
		initFilePrintItem();
		initFileExitItem();

		this.add(this.fileNewMenu);
		this.add(this.fileOpenItem);
		this.add(this.fileCloseItem);
		this.add(this.fileRecentMenu);
		this.add(this.fileSaveItem);
		this.add(this.fileSaveAsItem);
		this.add(this.fileExportMenu);
		this.add(this.filePrintItem);
		this.add(this.fileExitItem);
	}

	/**
	 * Add frame listener to detect closing request
	 */
	private void addWindowsClosingListener()
	{
		this.mainFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				stopper.exitProgram(mainFrame);
			}
		});
	}

	/**
	 * Init exit menu entry
	 */
	private void initFileExitItem()
	{
		this.fileExitItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				stopper.exitProgram(mainFrame);
			}
		});
		if (this.fileChooserService == null) this.fileExitItem.setEnabled(false);
	}

	/**
	 * Init export submenu
	 */
	private void initFileExportMenu()
	{
		initFileExportToImageItem();
		initFileExportToClipboardItem();
		initFileExportToJavaItem();
		initFileExportToPythonItem();


		this.fileExportMenu.add(this.fileExportToImageItem);
		this.fileExportMenu.add(this.fileExportToClipBoardItem);

		//TP3//
		initBannedNames();
		initValidTypes();
		initExportJavaCode();
		this.fileExportMenu.add(this.fileCodeItem);
		//TP3//

		// this.fileExportMenu.add(this.fileExportToJavaItem);
		// this.fileExportMenu.add(this.fileExportToPythonItem);

		if (this.fileChooserService == null) this.fileExportMenu.setEnabled(false);
	}

	/**
	 * Init export to python menu entry
	 */
	private void initFileExportToPythonItem()
	{
		this.fileExportToPythonItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
				}
			}
		});
	}

	/**
	 * Init export to java menu entry
	 */
	private void initFileExportToJavaItem()
	{
		this.fileExportToJavaItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
				}
			}
		});
	}

	/**
	 * Init export to clipboard menu entry
	 */
	private void initFileExportToClipboardItem()
	{
		this.fileExportToClipBoardItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					workspace.getGraphFile().exportToClipboard();
				}
			}
		});
	}

	/**
	 * Init export to image menu entry
	 */
	private void initFileExportToImageItem()
	{
		this.fileExportToImageItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					try
					{
						ExtensionFilter exportFilter = fileNamingService.getImageExtensionFilter();
						IFileWriter fileSaver = fileChooserService.chooseAndGetFileWriter(exportFilter);
						OutputStream out = fileSaver.getOutputStream();
						if (out != null)
						{
							String filename = fileSaver.getFileDefinition().getFilename();
							String extension = exportFilter.getExtension();
							if (filename.toLowerCase().endsWith(extension.toLowerCase()))
							{
								String format = extension.replace(".", "");
								workspace.getGraphFile().exportImage(out, format);
							}
						}
					}
					catch (Exception e1)
					{
						throw new RuntimeException(e1);
					}
				}
			}
		});
	}

	/**
	 * Init 'save as' menu entry
	 */
	private void initFileSaveAsItem()
	{
		this.fileSaveAsItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					IGraphFile graphFile = workspace.getGraphFile();
					graphFile.saveToNewLocation();
				}
			}
		});
		if (this.fileChooserService == null) this.fileSaveAsItem.setEnabled(false);
	}

	/**
	 * Init save menu entry
	 */
	private void initFileSaveItem()
	{
		this.fileSaveItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					workspace.getGraphFile().save();
				}
			}
		});
		if (this.fileChooserService == null || (this.fileChooserService != null && this.fileChooserService.isWebStart()))
		{
			this.fileSaveItem.setEnabled(false);
		}
	}

	/**
	 * Init print menu entry
	 */
	private void initFilePrintItem()
	{
		this.filePrintItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					workspace.getGraphFile().exportToPrinter();
				}
			}
		});
		if (this.fileChooserService == null) this.filePrintItem.setEnabled(false);
	}

	/**
	 * Init close menu entry
	 */
	private void initFileCloseItem()
	{
		this.fileCloseItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
				if (workspace != null)
				{
					IGraphFile graphFile = workspace.getGraphFile();
					if (graphFile.isSaveRequired())
					{
						JOptionPane optionPane = new JOptionPane();
						optionPane.setMessage(dialogCloseMessage);
						optionPane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
						optionPane.setIcon(dialogCloseIcon);
						dialogFactory.showDialog(optionPane, dialogCloseTitle, true);

						int result = JOptionPane.CANCEL_OPTION;
						if (!JOptionPane.UNINITIALIZED_VALUE.equals(optionPane.getValue()))
						{
							result = ((Integer) optionPane.getValue()).intValue();
						}

						if (result == JOptionPane.YES_OPTION)
						{
							String filename = graphFile.getFilename();
							if (filename == null)
							{
								graphFile.saveToNewLocation();
							}
							if (filename != null)
							{
								graphFile.save();
							}
							if (!graphFile.isSaveRequired())
							{
								mainFrame.removeDiagramPanel(workspace);
								userPreferencesService.removeOpenedFile(graphFile);
							}
						}
						if (result == JOptionPane.NO_OPTION)
						{
							mainFrame.removeDiagramPanel(workspace);
							userPreferencesService.removeOpenedFile(graphFile);
						}
					}
					if (!graphFile.isSaveRequired())
					{
						mainFrame.removeDiagramPanel(workspace);
						userPreferencesService.removeOpenedFile(graphFile);
					}
				}
			}
		});
	}

	/**
	 * Init open menu entry
	 */
	private void initFileOpenItem()
	{
		this.fileOpenItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					IFileReader fileOpener = fileChooserService.chooseAndGetFileReader();
					if (fileOpener == null)
					{
						// Action cancelled by user
						return;
					}
					IFile selectedFile = fileOpener.getFileDefinition();
					IGraphFile graphFile = new GraphFile(selectedFile);
					IWorkspace workspace = new Workspace(graphFile);
					mainFrame.addTabbedPane(workspace);
					userPreferencesService.addOpenedFile(graphFile);
					userPreferencesService.addRecentFile(graphFile);
				}
				catch (IOException e)
				{
					dialogFactory.showWarningDialog(e.getMessage());
				}
			}
		});
		if (this.fileChooserService == null) this.fileOpenItem.setEnabled(false);
	}

	/**
	 * Init new menu entry
	 */
	public void initFileNewMenu()
	{
		List<IDiagramPlugin> diagramPlugins = this.pluginRegistry.getDiagramPlugins();

		// Step 1 : sort diagram plugins by categories and names
		SortedMap<String, SortedSet<IDiagramPlugin>> diagramPluginsSortedByCategory = new TreeMap<String, SortedSet<IDiagramPlugin>>();
		for (final IDiagramPlugin aDiagramPlugin : diagramPlugins)
		{
			String category = aDiagramPlugin.getCategory();
			if (!diagramPluginsSortedByCategory.containsKey(category))
			{
				SortedSet<IDiagramPlugin> newSortedSet = new TreeSet<IDiagramPlugin>(new Comparator<IDiagramPlugin>()
						{
					@Override
					public int compare(IDiagramPlugin o1, IDiagramPlugin o2)
					{
						String n1 = o1.getName();
						String n2 = o2.getName();
						return n1.compareTo(n2);
					}
						});
				diagramPluginsSortedByCategory.put(category, newSortedSet);
			}
			SortedSet<IDiagramPlugin> aSortedSet = diagramPluginsSortedByCategory.get(category);
			aSortedSet.add(aDiagramPlugin);
		}

		// Step 2 : populate menu entry
		for (String aCategory : diagramPluginsSortedByCategory.keySet()) {
			String categoryName = aCategory.replaceFirst("[0-9]*\\.", "");
			JMenu categoryMenuItem = new JMenu(categoryName);
			fileNewMenu.add(categoryMenuItem);
			SortedSet<IDiagramPlugin> diagramPluginsByCategory = diagramPluginsSortedByCategory.get(aCategory);
			for (final IDiagramPlugin aDiagramPlugin : diagramPluginsByCategory)
			{
				String name = aDiagramPlugin.getName();
				name = name.replaceFirst("[0-9]*\\.", "");
				JMenuItem item = new JMenuItem(name);
				item.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent event)
					{
						Class<? extends IGraph> graphClass = aDiagramPlugin.getGraphClass();
						IGraphFile graphFile = new GraphFile(graphClass);
						IWorkspace diagramPanel = new Workspace(graphFile);
						mainFrame.addTabbedPane(diagramPanel);
					}
				});
				categoryMenuItem.add(item);
			}
		}
	}

	/**
	 * Init recent menu entry
	 */
	public void initFileRecentMenu()
	{
		// Set entries on startup
		refreshFileRecentMenu();
		// Refresh recent files list each time the global file menu gets the focus
		this.addFocusListener(new FocusListener()
		{

			public void focusGained(FocusEvent e)
			{
				refreshFileRecentMenu();
			}

			public void focusLost(FocusEvent e)
			{
				// Nothing to do
			}

		});
		if (this.fileChooserService == null || (this.fileChooserService != null && this.fileChooserService.isWebStart()))
		{
			this.fileRecentMenu.setEnabled(false);
		}
	}

	/**
	 * Updates file recent menu
	 */
	private void refreshFileRecentMenu()
	{
		fileRecentMenu.removeAll();
		for (final IFile aFile : userPreferencesService.getRecentFiles())
		{
			String name = aFile.getFilename();
			JMenuItem item = new JMenuItem(name);
			fileRecentMenu.add(item);
			item.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					try
					{
						IGraphFile graphFile = new GraphFile(aFile);
						IWorkspace workspace = new Workspace(graphFile);
						mainFrame.addTabbedPane(workspace);
					}
					catch (IOException e)
					{
						dialogFactory.showErrorDialog(e.getMessage());
					}
				}
			});
		}
	}

	/** The file chooser to use with with menu */
	@InjectedBean
	private IFileChooserService fileChooserService;

	/** Application stopper */
	private ApplicationStopper stopper = new ApplicationStopper();

	/** Plugin registry */
	@InjectedBean
	private PluginRegistry pluginRegistry;

	/** DialogBox handler */
	@InjectedBean
	private DialogFactory dialogFactory;

	/** Access to user preferences */
	@InjectedBean
	private UserPreferencesService userPreferencesService;

	/** File services */
	@InjectedBean
	private FileNamingService fileNamingService;

	/** Application main frame */
	private MainFrame mainFrame;

	@ResourceBundleBean(key = "file.new")
	private JMenu fileNewMenu;

	@ResourceBundleBean(key = "file.open")
	private JMenuItem fileOpenItem;

	@ResourceBundleBean(key = "file.recent")
	private JMenu fileRecentMenu;

	@ResourceBundleBean(key = "file.close")
	private JMenuItem fileCloseItem;

	@ResourceBundleBean(key = "file.save")
	private JMenuItem fileSaveItem;

	@ResourceBundleBean(key = "file.save_as")
	private JMenuItem fileSaveAsItem;

	@ResourceBundleBean(key = "file.export_to_image")
	private JMenuItem fileExportToImageItem;

	@ResourceBundleBean(key = "file.export_to_clipboard")
	private JMenuItem fileExportToClipBoardItem;

	@ResourceBundleBean(key = "file.export_to_java")
	private JMenuItem fileExportToJavaItem;

	@ResourceBundleBean(key = "file.export_to_python")
	private JMenuItem fileExportToPythonItem;

	@ResourceBundleBean(key = "file.export")
	private JMenu fileExportMenu;

	@ResourceBundleBean(key = "file.print")
	private JMenuItem filePrintItem;

	@ResourceBundleBean(key = "file.exit")
	private JMenuItem fileExitItem;

	@ResourceBundleBean(key = "dialog.close.title")
	private String dialogCloseTitle;

	@ResourceBundleBean(key = "dialog.close.ok")
	private String dialogCloseMessage;

	@ResourceBundleBean(key = "dialog.close.icon")
	private ImageIcon dialogCloseIcon;

	//TP3//

	private JMenuItem fileCodeItem = new JMenuItem("Exporter le code Java");
	private ArrayList<String> bannedNames = new ArrayList<String>();
	private ArrayList<String> validTypes = new ArrayList<String>();
	
	/**
	 * cree le bouton "Exporter le code Java"
	 */
	private void initExportJavaCode (){
		this.fileCodeItem.addActionListener(new ActionListener (){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Exporter le code Java...");
				boolean printCode = true;
				IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();

				if(!workspace.getGraphFile().getGraph().getClass().toString().equals("class com.horstmann.violet.product.diagram.classes.ClassDiagramGraph"))
						System.out.println("Wrong Diagram");
				else{ 
					resetColors();
					//classnode check
					for(INode n : workspace.getGraphFile().getGraph().getAllNodes()){
						if(n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.PackageNode")){
							printCode = false;
							System.out.println("PackageNode Unsupported");
							break;
						}
						else if(n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.InterfaceNode")){
							printCode = false;
							System.out.println("InterfaceNode Unsupported");
							break;
						}
						else if(!n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.ClassNode")){
							continue;
						}
						
						ClassNode n2 = (ClassNode) n;
						if(!validateClassNode(n2)) 
							printCode = false;
						
						//System.out.println(n2.getName() + "+" + n2.getValid());
					}
					
					
					for (IEdge edge : workspace.getGraphFile().getGraph().getAllEdges()){
						if(!validateEdge(edge)){
							SegmentedLineEdge ed = (SegmentedLineEdge) edge;
							printCode = false;
							ed.setColor(Color.RED);
							ed.draw(ed.getG());
						}
					}
					
					//Genere du code java s'il n'y a pas d'erreur
					if(printCode){
						JFileChooser jfc = new JFileChooser();
						jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						jfc.showOpenDialog(mainFrame);
						
						File dir = jfc.getSelectedFile();
						try {
							generateCode(dir,  workspace.getGraphFile().getGraph().getAllNodes(), workspace.getGraphFile().getGraph().getAllEdges());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						System.out.println("Success");
					}
					else
						System.out.println("Code non genere a cause d'une erreur");
				}
			}
		});
	}
	
	/**
	 * Genere du code java dans un fichier donnee
	 * @param dir
	 * @param nodeList
	 * @param edgeList
	 * @throws IOException
	 */
	private void generateCode(File dir, Collection<INode> nodeList, Collection<IEdge> edgeList) throws IOException{
		
		for(INode n : nodeList){
			if(!n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.ClassNode")){
				continue;
			}
			ClassNode n2 = (ClassNode) n;
			File f = new File(dir + "\\" + n2.getName() + ".java");
			if(!f.exists())
				f.createNewFile();
			
			String name = "";
			String attributes = "";
			String methods = "";
			String ret = "";
			
			//ecrire le nom
			name += getNameCode(n2, edgeList);
			
			//ecrire les attributs
			attributes = getAttributeCode(n2, edgeList);
			
			//ecrire les methodes
			methods = getMethodCode(n2);

			//ecrire dans le fichier
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write(name + "{\n" + attributes + methods + "\n}");
			out.newLine();
			out.close();
		}
	}
	
	/**
	 * ecrit le nom
	 * @param n
	 * @param edgeList
	 * @return
	 */
	private String getNameCode(ClassNode n, Collection<IEdge> edgeList){
		String name = "public class " + n.getName().toString();
		
		for(IEdge ed : edgeList){
			if(ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.InheritanceEdge")){
				InheritanceEdge inEd = (InheritanceEdge) ed;
				ClassNode start = (ClassNode) inEd.getStart();
				ClassNode end = (ClassNode) inEd.getEnd();
				if(start.getName().toString().equals(n.getName().toString())){
					name += " extends "+ end.getName().toString();
				}
			}
		}
		return name;
	}
	
	/**
	 * ecrit les methodes
	 * @param n
	 * @param edgeList
	 * @return
	 */
	private String getAttributeCode(ClassNode n, Collection<IEdge> edgeList){
		String attributes = "";
		int count = 0;
		//ecrit les attributs (pas selon les edges)
		String[] attrList = n.getAttributes().toString().split("\\|");
		for (int i = 0; i<attrList.length; i++){
			if(attrList[i].length()>1){
				if(attrList[i].charAt(0) == '+') attributes += "\tpublic";
				else if(attrList[i].charAt(0) == '-') attributes += "\tprivate";
				else if(attrList[i].charAt(0) == '~') attributes += "\tpackage";	
				else if(attrList[i].charAt(0) == '#') attributes += "\tprotected";
			
				attributes += attrList[i].substring(attrList[i].indexOf(":")+1) + " ";
				attributes += attrList[i].substring(1, attrList[i].indexOf(":"));

				attributes += ";\n";
			}
		}
		

		for(IEdge ed: edgeList){
			if(ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AssociationEdge")||
			   ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AggregationEdge")||
			   ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.CompositionEdge")){
				
				AssociationEdge assEd = null;
				AggregationEdge aggEd = null;
				CompositionEdge comEd = null;
				
				//verifie quel type de edge
				if(ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AssociationEdge")){
					assEd = (AssociationEdge) ed;
				}
				else if(ed.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AggregationEdge")){
					aggEd = (AggregationEdge) ed;
				}
				else {
					comEd = (CompositionEdge) ed;
				}
				
				ClassNode start = (ClassNode) ed.getStart();
				ClassNode end = (ClassNode) ed.getEnd();
				
				//verifier cardinalite des edges et ecrit selon
				if(start.getName().toString().equals(n.getName().toString())){
					if(assEd != null){
						if(assEd.getEndLabel().toString().equals("1")||assEd.getEndLabel().toString().equals("0..1"))
							attributes += "\tprivate "+ end.getName().toString() + " " + end.getName().toString().toLowerCase()+ ++count + ";\n";
						else
							attributes += "\tprivate "+ "Collection<"+end.getName().toString() + "> " + end.getName().toString().toLowerCase()+ ++count + ";\n";
					}
					else if(aggEd != null){
						if(aggEd.getEndLabel().toString().equals("1")||aggEd.getEndLabel().toString().equals("0..1"))
							attributes += "\tprivate "+ end.getName().toString() + " " + end.getName().toString().toLowerCase()+ ++count + ";\n";
						else
							attributes += "\tprivate "+ "Collection<"+end.getName().toString() + "> " + end.getName().toString().toLowerCase()+ ++count + ";\n";
					}
					else if(comEd != null){
						if(comEd.getEndLabel().toString().equals("1")||comEd.getEndLabel().toString().equals("0..1"))
							attributes += "\tprivate "+ end.getName().toString() + " " + end.getName().toString().toLowerCase()+ ++count + ";\n";
						else
							attributes += "\tprivate "+ "Collection<"+end.getName().toString() + "> " + end.getName().toString().toLowerCase()+ ++count + ";\n";
					}
				}
			}
		}
		
		return attributes;
	}
	/**
	 * ecrit les methodes
	 * @param n2
	 * @return
	 */
	private String getMethodCode(ClassNode n2){
		String methods = "";
		String ret = "";
		String[] methList = n2.getMethods().toString().split("\\|");
		for(int i =0; i<methList.length;i++){
			ret = "";
			//ecrire les methodes
			if(methList[i].length()>0){
				if(methList[i].charAt(0) == '+') methods += "\tpublic";
				else if(methList[i].charAt(0) == '-') methods += "\tprivate";
				else if(methList[i].charAt(0) == '~') methods += "\tpackage";	
				else if(methList[i].charAt(0) == '#') methods += "\tprotected";
				
				methods += methList[i].substring(methList[i].lastIndexOf(":")+1) + " ";

				//verifier s'il faut mettre un return dans la methode
				Pattern pat = Pattern.compile(".*void.*");
				Matcher mat = pat.matcher(methList[i].substring(methList[i].lastIndexOf(":")+1));
				boolean vd = mat.matches();
				if(!vd){
					ret = "return null;";
				}
				
				methods += methList[i].substring(1, methList[i].indexOf("("));
				
				methods += "(";
				String[] paramList = methList[i].substring(methList[i].indexOf("(")+1, methList[i].lastIndexOf(")")).split(",");
				if(paramList.length >0){
					for (int j = 0; j< paramList.length; j++){
						if(paramList[j].length()>0){
							methods += paramList[j].substring(paramList[j].indexOf(":")+1) + " ";
							methods += paramList[j].substring(0, paramList[j].lastIndexOf(":"));
						}
					}
				}
				methods += "){"+ ret +"}\n";
			}
		}
		
		return methods;
	}
	/**
	 * initialise la liste pour les noms invalides
	 */
	private void initBannedNames() {
		bannedNames.add("abstract");
		bannedNames.add("assert");
		bannedNames.add("boolean");
		bannedNames.add("break");
		bannedNames.add("byte");
		bannedNames.add("case");
		bannedNames.add("catch");
		bannedNames.add("char");
		bannedNames.add("class");
		bannedNames.add("const");
		bannedNames.add("continue");
		bannedNames.add("default");
		bannedNames.add("do");
		bannedNames.add("double");
		bannedNames.add("else");
		bannedNames.add("enum");
		bannedNames.add("extends");
		bannedNames.add("final");
		bannedNames.add("finally");
		bannedNames.add("float");
		bannedNames.add("for");
		bannedNames.add("goto");
		bannedNames.add("if");
		bannedNames.add("implements");
		bannedNames.add("import");
		bannedNames.add("instanceof");
		bannedNames.add("int");
		bannedNames.add("interface");
		bannedNames.add("long");
		bannedNames.add("native");
		bannedNames.add("new");
		bannedNames.add("package");
		bannedNames.add("private");
		bannedNames.add("public");
		bannedNames.add("protected");
		bannedNames.add("return");
		bannedNames.add("short");
		bannedNames.add("static");
		bannedNames.add("strictfp");
		bannedNames.add("super");
		bannedNames.add("switch");
		bannedNames.add("synchronized");
		bannedNames.add("this");
		bannedNames.add("throw");
		bannedNames.add("throws");
		bannedNames.add("transient");
		bannedNames.add("try");
		bannedNames.add("void");
		bannedNames.add("volatile");
		bannedNames.add("while");
	}
	
	/**
	 * initialise la liste des types primitifs
	 */
	private void initValidTypes(){
		validTypes.add("boolean");
		validTypes.add("type");
		validTypes.add("char");
		validTypes.add("double");
		validTypes.add("float");
		validTypes.add("int");
		validTypes.add("long");
		validTypes.add("short");
	}
	
	/**
	 * Verifie si le Classnode est correct
	 * @param n
	 * @return
	 */
	private boolean validateClassNode(ClassNode n){
		n.setValid(true);
		if(!validateName(n.getName().toString())){
			n.setValid(false);
		}
		if(!validateAttributes(n.getAttributes().toString())){
			n.setValid(false);
		}
		if(!validateMethods(n.getMethods().toString())){
			n.setValid(false);
		}
		n.draw(n.getG());
		return n.getValid();
	}

	/**
	 * verifie si le edge est correct
	 * @param edge
	 * @return
	 */
	private boolean validateEdge(IEdge edge){
		boolean valid = true;
		//assoc edge
		if(edge.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AssociationEdge")){
			AssociationEdge ed = (AssociationEdge) edge;
			if(ed.getMiddleLabel().toString().length()==0){
				valid = false;
			}
			
			if(valid){
				Pattern p = Pattern.compile("\\*||[0-9]+");
				Matcher m = p.matcher(ed.getEndLabel().toString());
				valid = m.matches();
			}
		}
		//composition edge
		else if(edge.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.CompositionEdge")){
			CompositionEdge ed = (CompositionEdge) edge;
			if(ed.getMiddleLabel().toString().length()==0){
				valid = false;
			}
			
			if(valid){
				Pattern p = Pattern.compile("\\*||[0-9]+");
				Matcher m = p.matcher(ed.getEndLabel().toString());
				valid = m.matches();
			}
		}
		//aggregation edge
		else if(edge.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.edges.AggregationEdge")){
			AggregationEdge ed = (AggregationEdge) edge;
			if(ed.getMiddleLabel().toString().length()==0){
				valid = false;
			}
			
			if(valid){
				Pattern p = Pattern.compile("\\*||[0-9]+");
				Matcher m = p.matcher(ed.getEndLabel().toString());
				valid = m.matches();
			}
		}
		//inheritance edge
		else{
			InheritanceEdge ed = (InheritanceEdge) edge;
			INode start = ed.getStart();
			INode end = ed.getEnd();
			IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
			for(IEdge n : workspace.getGraphFile().getGraph().getAllEdges()){	
				if(!n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.InheritanceEdge")){
					INode start2 = n.getStart();
					INode end2 = n.getEnd();
					if(end.equals(start2) && start.equals(end2)){
						valid = false;
						break;
					}
				}
			}
		}
		return valid;
	}
	
	/**
	 * Remet toutes les bordures a la couleure noir
	 */
	private void resetColors(){
		IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
		for(INode n : workspace.getGraphFile().getGraph().getAllNodes()){	
			if(!n.getClass().getName().equals("com.horstmann.violet.product.diagram.classes.nodes.ClassNode")){
				continue;
			}
			ClassNode n2 = (ClassNode) n;
			n2.setValid(true);
			n2.draw(n2.getG());
		}
		
		for(IEdge edge: workspace.getGraphFile().getGraph().getAllEdges()){
			SegmentedLineEdge ed = (SegmentedLineEdge)edge;
			ed.setColor(Color.BLACK);
			ed.draw(ed.getG());
		}
	}
	
	/**
	 * Verify que le nom de classe, d'attribut ou de methode est valide
	 * @param s
	 * @return boolean valid
	 */
	private boolean validateName(String s){
		boolean valid = true;
		Pattern p = Pattern.compile("(_|\\$|[a-z]|[A-Z])(_|\\$|[a-z]|[A-Z]|\\d)*");
		Matcher m = p.matcher(s);
		valid = m.matches();
		if (valid){
			for (int i = 0; i<bannedNames.size(); i++){
				if(s.equals(bannedNames.get(i))){
					valid = false;
					break;
				}
			}
		}
		return valid;
	}
	
	/**
	 * indique si le type est valide
	 * @param s
	 * @return
	 */
	private boolean isType(String s){
		boolean valid = false;

		//types primitifs
		for(int i = 0; i<validTypes.size();i++){
			if(s.equals(validTypes.get(i))){
				valid = true;
				break;
			}
		}
		
		//types objets
		if(!valid){
			Pattern p = Pattern.compile(" *[A-Z]([a-zA-Z0-9])* *");
			Matcher m = p.matcher(s);
			valid = m.matches();
		}
		
		//type objet avec les points
		if(!valid){
			Pattern p = Pattern.compile(" *[a-z]((a-zA-Z0-9)*.)+[A-Z](a-zA-Z0-9) *");
			Matcher m = p.matcher(s);
			valid = m.matches();
		}
		
		//tableau
		if(!valid){
			Pattern p = Pattern.compile(" *[a-zA-Z](a-zA-Z0-9)*\\[ *\\] *");
			Matcher m = p.matcher(s);
			valid = m.matches();
		}
		
		//generics de java
		if(!valid){
			Pattern p = Pattern.compile(" *[A-Z]([a-zA-Z])* *<.*> *");
			Matcher m = p.matcher(s);
			valid = m.matches();
		}

		return valid;
	}
	
	/**
	 * indique si l'attribut est valide
	 * @param s
	 * @return
	 */
	private boolean validateAttributes(String s){
		boolean valid = true;
		String[] s2 = new String[s.split("|").length];
		s2 = s.split("\\|");
		
		for(int i = 0; i< s2.length; i++){
			if(s2[i].length()==0){
				continue;
			}
			else{
				if(!isVisible(s2[i]))
					valid = false;
				else{
					String nameSs = s2[i].substring(1, s2[i].indexOf(":"));
					String typeSs = s2[i].substring(s2[i].indexOf(":")+1);
					if(!validateName(nameSs))
						valid = false;
					else if(!isType(typeSs))
						valid = false;
				}
			}
				
			//System.out.println(s2[i]);
		}
		
		return valid;
	}
	
	/**
	 * verifie si la methode est valide
	 * @param s
	 * @return
	 */
	private boolean validateMethods(String s){
		boolean valid = true;
		String[] s2 = new String[s.split("|").length];
		s2 = s.split("\\|");
		
		for(int i = 0; i< s2.length; i++){
			if(s2[i].length()==0){
				continue;
			}
			else{
				//verifier visibilite + - ~ #
				if(!isVisible(s2[i]))
					valid = false;
				//verifier la forme de la methode a([b]):c
				else{
					Pattern p = Pattern.compile(".*(.*) *:.*");
					Matcher m = p.matcher(s2[i]);
					valid = m.matches();
					
					//verifier les noms et les retours valides
					if(valid){
						String nameSs = s2[i].substring(1, s2[i].indexOf('('));
						String returnSs = s2[i].substring(s2[i].lastIndexOf(':') + 1);
						if(!validateName(nameSs))
							valid = false;
						else if(!isType(returnSs)){
							p = Pattern.compile(" *void");
							m = p.matcher(returnSs);
							valid = m.matches();
						}
					}
					
					//verifier les parametres
					if(valid){
						String paraSs = s2[i].substring(s2[i].indexOf('(')+1, s2[i].lastIndexOf(')'));
						valid = isPara(paraSs);
					}
				}
			}
				
			//System.out.println(s2[i]);
		}
		return valid;
	}
	
	/**
	 * verifie si les parametres sont valides
	 * @param s
	 * @return
	 */
	private boolean isPara(String s){
		
		boolean valid = true;
		
		if(s.length() == 0)
			valid = true;
		else{
			String[] s2 = s.split(",");
			//verifier que les parametres sont bien de la forme nom:type
			for(int i = 0; i < s2.length; i++){
				Pattern p = Pattern.compile(".*:.*");
				Matcher m = p.matcher(s2[i]);
				valid = m.matches();
				if(!valid)
					break;
				
				//verifier que le nom et le type sont valides
				else{
					String nameSs = s2[i].substring(0, s2[i].lastIndexOf(":"));
					String typeSs = s2[i].substring(s2[i].lastIndexOf(":")+1);
					if(!validateName(nameSs)){
						valid = false;
						break;		
					}
					if(!isType(typeSs)){
						valid = false;
						break;
					}
				}
			}
		}
		return valid;
	}

	/**
	 * verifie si la visibilite est correct (+,-,#,~)
	 * @param s
	 * @return
	 */
	private boolean isVisible(String s){
		boolean valid = true;
		Pattern p = Pattern.compile("(\\-.*:.*|\\+.*:.*|\\~.*:.*|\\#.*:.*)");
		Matcher m = p.matcher(s);
		valid = m.matches();
		return valid;
	}
	//TP3//
}
