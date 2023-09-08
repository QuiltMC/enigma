/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 * Copyright 2011-2022 Hanns Holger Rutz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.gui.syntax;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The DefaultSyntaxKit is the main entry to SyntaxPane.  To use the package, just
 * set the EditorKit of the EditorPane to a new instance of this class.
 *
 * You need to pass a proper lexer to the class.
 *
 * @author ayman, Hanns Holger Rutz
 */
public class DefaultSyntaxKit extends DefaultEditorKit implements ViewFactory {

	public static final String CONFIG_CARETCOLOR = "CaretColor";
	public static final String CONFIG_ENABLE_WORD_WRAP = "EnableWordWrap";
	public static final String CONFIG_SELECTION = "SelectionColor";
	public static final String CONFIG_COMPONENTS = "Components";
	public static final String CONFIG_MENU = "PopupMenu";
	public static final String CONFIG_TOOLBAR = "Toolbar";
	public static final String CONFIG_TOOLBAR_ROLLOVER = "Toolbar.Buttons.Rollover";
	public static final String CONFIG_TOOLBAR_BORDER = "Toolbar.Buttons.BorderPainted";
	public static final String CONFIG_TOOLBAR_OPAQUE = "Toolbar.Buttons.Opaque";
	public static final String CONFIG_TOOLBAR_BORDER_SIZE = "Toolbar.Buttons.BorderSize";
	private static final Pattern ACTION_KEY_PATTERN = Pattern.compile("Action\\.((\\w|-)+)");
	private static final Pattern DEFAULT_ACTION_PATTERN = Pattern.compile("(DefaultAction.((\\w|-)+)).*");
	private static Font DEFAULT_FONT;
	private static final Set<String> CONTENT_TYPES = new HashSet<String>();
	private static Boolean initialized = false;
	private static Map<String, String> abbreviations;
	private static String MENU_MASK_STRING = "control ";
	private final Lexer lexer;
	private static final Logger LOG = Logger.getLogger(DefaultSyntaxKit.class.getName());
	private final Map<JEditorPane, List<SyntaxComponent>> editorComponents =
		new WeakHashMap<JEditorPane, List<SyntaxComponent>>();
	private final Map<JEditorPane, JPopupMenu> popupMenu =
		new WeakHashMap<JEditorPane, JPopupMenu>();
	/**
	 * Main Configuration of SyntaxPane EditorKits
	 */
	private static Map<Class<? extends DefaultSyntaxKit>, Configuration> CONFIGS;

	private static final String PLATFORM_KEY;

	static {
		// we only need to initialize once.
		if (!initialized) {
			initKit();
		}
		int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		if ((menuMask & (KeyEvent.ALT_DOWN_MASK | KeyEvent.ALT_MASK)) != 0) {
			MENU_MASK_STRING = "alt ";
		} else if ((menuMask & (KeyEvent.META_DOWN_MASK | KeyEvent.META_MASK)) != 0) {
			MENU_MASK_STRING = "meta ";
		}

		String osName = System.getProperty("os.name");
		if (osName.contains("Linux"))
			PLATFORM_KEY = ".LinuxKey";
		else if (osName.contains("Mac"))
			PLATFORM_KEY = ".MacKey";
		else
			PLATFORM_KEY = ".WindowsKey";
	}
	private static final String ACTION_MENU_TEXT = "MenuText";

	/**
	 * Creates a new Kit for the given language
	 */
	public DefaultSyntaxKit(Lexer lexer) {
		super();
		this.lexer = lexer;
	}

	/**
	 * Adds UI components to the pane
	 *
	 * @param editorPane    a component to install this kit for
	 */
	public void addComponents(JEditorPane editorPane) {
		// install the components to the editor:
		String[] components = this.getConfig().getPropertyList(CONFIG_COMPONENTS);
		for (String c : components) {
			this.installComponent(editorPane, c);
		}
	}

	/**
	 * Creates a SyntaxComponent of the the given class name and installs it on the pane
	 */
	public void installComponent(JEditorPane pane, String className) {
		try {
			Class<?> compClass = Class.forName(className);
			SyntaxComponent comp = (SyntaxComponent) compClass.newInstance();
			comp.config(this.getConfig());
			comp.install(pane);
			this.editorComponents.computeIfAbsent(pane, k -> new ArrayList<>());
			this.editorComponents.get(pane).add(comp);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Finds the SyntaxComponent with given class name that is installed
	 * on the given pane, then de-installs and removes it fom the
	 * editorComponents list
	 */
	public void deinstallComponent(JEditorPane pane, String className) {
		for (SyntaxComponent c : this.editorComponents.get(pane)) {
			if (c.getClass().getName().equals(className)) {
				c.deinstall(pane);
				this.editorComponents.get(pane).remove(c);
				break;
			}
		}
	}

	/**
	 * Checks if the component with given class-name is installed on the pane.
	 *
	 * @return true if component is installed, false otherwise
	 */
	public boolean isComponentInstalled(JEditorPane pane, String className) {
		for (SyntaxComponent c : this.editorComponents.get(pane)) {
			if (c.getClass().getName().equals(className)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Toggles the component with given class-name.  If component is found
	 * and installed, then it is uninstalled.  Otherwise a new one is
	 * installed
	 *
	 * @return true if component was installed, false if it was removed
	 */
	public boolean toggleComponent(JEditorPane pane, String className) {
		for (SyntaxComponent c : this.editorComponents.get(pane)) {
			if (c.getClass().getName().equals(className)) {
				c.deinstall(pane);
				this.editorComponents.get(pane).remove(c);
				return false;
			}
		}
		this.installComponent(pane, className);
		return true;
	}

	@Override
	public void read(Reader in, Document doc, int pos)
		throws IOException, BadLocationException {
		super.read(in, doc, pos);
		// ((SyntaxDocument) doc).clearUndos();
	}

	/**
	 * Adds a popup menu to the editorPane if needed.
	 */
	public void addPopupMenu(JEditorPane editorPane) {
		String[] menuItems = this.getConfig().getPropertyList(CONFIG_MENU);
		if (menuItems == null || menuItems.length == 0) {
			return;
		}
		this.popupMenu.put(editorPane, new JPopupMenu());
		JMenu stack = null;
		for (String menuString : menuItems) {

			// create the Popup menu
			if (menuString.equals("-")) {
				this.popupMenu.get(editorPane).addSeparator();
			} else if (menuString.startsWith(">")) {
				JMenu sub = new JMenu(menuString.substring(1));
				this.popupMenu.get(editorPane).add(sub);
				stack = sub;
			} else if (menuString.startsWith("<")) {
				Container parent = stack == null ? null : stack.getParent();
				if (parent instanceof JMenu) {
					stack = (JMenu) parent;
				} else {
					stack = null;
				}
			} else {
				Action action = editorPane.getActionMap().get(menuString);
				if (action != null) {
					JMenuItem menuItem;
					if (action.getValue(Action.SELECTED_KEY) != null) {
						menuItem = new JCheckBoxMenuItem(action);
					} else {
						menuItem = new JMenuItem(action);
					}
					// Use our own property if it was set for the menu text
					if (action.getValue(ACTION_MENU_TEXT) != null) {
						menuItem.setText((String) action.getValue(ACTION_MENU_TEXT));
					}
					if (stack == null) {
						this.popupMenu.get(editorPane).add(menuItem);
					} else {
						stack.add(menuItem);
					}
				}
			}
		}
		editorPane.setComponentPopupMenu(this.popupMenu.get(editorPane));
	}

	/**
	 * Adds all pop-up menu items to a Toolbar.  <b>You need to call the validate method
	 * on the toolbar after this is done to layout the buttons.</b>
	 * Only Actions which have a SMALL_ICON property will be added to the toolbar
	 * There are three Configuration Keys that affect the appearance of the added buttons:
	 * CONFIG_TOOLBAR_ROLLOVER, CONFIG_TOOLBAR_BORDER, CONFIG_TOOLBAR_OPAQUE
	 */
	public void addToolBarActions(JEditorPane editorPane, JToolBar toolbar) {
		String[] toolBarItems = this.getConfig().getPropertyList(CONFIG_TOOLBAR);
		if (toolBarItems == null || toolBarItems.length == 0) {
			toolBarItems = this.getConfig().getPropertyList(CONFIG_MENU);
			if (toolBarItems == null || toolBarItems.length == 0) {
				return;
			}
		}
		boolean btnRolloverEnabled = this.getConfig().getBoolean(CONFIG_TOOLBAR_ROLLOVER, true);
		boolean btnBorderPainted = this.getConfig().getBoolean(CONFIG_TOOLBAR_BORDER, false);
		boolean btnOpaque = this.getConfig().getBoolean(CONFIG_TOOLBAR_OPAQUE, false);
		int btnBorderSize = this.getConfig().getInteger(CONFIG_TOOLBAR_BORDER_SIZE, 2);
		for (String menuString : toolBarItems) {
			if (menuString.equals("-") ||
				menuString.startsWith("<") ||
				menuString.startsWith(">")) {
				toolbar.addSeparator();
			} else {
				Action action = editorPane.getActionMap().get(menuString);
				if (action != null && action.getValue(Action.SMALL_ICON) != null) {
					JButton b = toolbar.add(action);
					b.setRolloverEnabled(btnRolloverEnabled);
					b.setBorderPainted(btnBorderPainted);
					b.setOpaque(btnOpaque);
					b.setFocusable(false);
					b.setBorder(BorderFactory.createEmptyBorder(btnBorderSize,
						btnBorderSize, btnBorderSize, btnBorderSize));
				}
			}
		}
	}

	@Override
	public ViewFactory getViewFactory() {
		return this;
	}

	@Override
	public View create(Element element) {
		Configuration conf = this.getConfig();
		return new SyntaxView(element, conf);
	}

	/**
	 * Installs the View on the given EditorPane.  This is called by Swing and
	 * can be used to do anything you need on the JEditorPane control.  Here
	 * I set some default Actions.
	 */
	@Override
	public void install(JEditorPane editorPane) {
		super.install(editorPane);
		// get our font
		String fontName = this.getProperty("DefaultFont");
		Font font = DEFAULT_FONT;
		if (fontName != null) {
			font = Font.decode(fontName);
		}
		editorPane.setFont(font);
		Configuration conf = this.getConfig();
		Color caretColor = conf.getColor(CONFIG_CARETCOLOR, Color.BLACK);
		editorPane.setCaretColor(caretColor);
		Color selectionColor = this.getConfig().getColor(CONFIG_SELECTION, new Color(0x99ccff));
		editorPane.setSelectionColor(selectionColor);
		this.addActions(editorPane);
		this.addComponents(editorPane);
		this.addPopupMenu(editorPane);
	}

	@Override
	public void deinstall(JEditorPane editorPane) {
		for (SyntaxComponent c : this.editorComponents.get(editorPane)) {
			c.deinstall(editorPane);
		}
		this.editorComponents.clear();
		editorPane.getInputMap().clear();
		ActionMap m = editorPane.getActionMap();
		for (Object key : editorPane.getActionMap().keys()) {
			Action a = m.get(key);
			if (a instanceof SyntaxAction) {
				((SyntaxAction) a).deinstall(editorPane);
			}
		}
		m.clear();
	}

	/**
	 * Adds keyboard actions to this control using the Configuration we have
	 * This is revised to properly use InputMap and ActionMap of the component
	 * instead of using the KeyMaps directly.
	 *
	 * @param editorPane    the component to attach the actions to
	 */
	public void addActions(JEditorPane editorPane) {
		InputMap imap = new InputMap();
		imap.setParent(editorPane.getInputMap());
		ActionMap amap = new ActionMap();
		amap.setParent(editorPane.getActionMap());

		for (Configuration.StringKeyMatcher m : this.getConfig().getKeys(ACTION_KEY_PATTERN)) {
			String[] values = Configuration.COMMA_SEPARATOR.split(
				m.value);
			String actionClass = values[0];
			String actionName = m.group1;
			SyntaxAction action = this.createAction(actionClass);
			// The configuration keys will need to be prefixed by Action
			// to make it more readable in the Configuration files.
			action.install(editorPane, this.getConfig(), DefaultSyntaxAction.ACTION_PREFIX + actionName);
			// Add the action to the component also
			amap.put(actionName, action);
			// Now bind all the keys to the Action we have using the InputMap
			String platformKey = this.getProperty(m.key + PLATFORM_KEY);
			for (int i = 1; i < values.length; i++) {
				String ksString         = values[i].replace("menu ", MENU_MASK_STRING);
				String keyStrokeString  = platformKey == null ? ksString : platformKey;
				KeyStroke ks = KeyStroke.getKeyStroke(keyStrokeString);
				// we may have more than onr value ( for key action ), but we will use the
				// last one in the single value here.  This will display the key in the
				// popup menus.  Pretty neat.
				if (ks == null) {
					throw new IllegalArgumentException("Invalid KeyStroke: " +
						keyStrokeString);
				}
				action.putValue(Action.ACCELERATOR_KEY, ks);
				imap.put(ks, actionName);
			}
		}

		// Now configure the Default actions for better display in the popup menu
		for (Configuration.StringKeyMatcher m : this.getConfig().getKeys(DEFAULT_ACTION_PATTERN)) {
			String name = m.matcher.group(2);
			Action action = editorPane.getActionMap().get(name);
			if (action != null) {
				this.configActionProperties(action, name, m.group1);
			}
			// The below commented block does find the keys for the default Actions
			// using InputMap, however there are multiple bound keys for the
			// default actions that displaying them in the menu will probably not
			// be the most obvious binding
            /*
            for (KeyStroke key : imap.allKeys()) {
            Object o = imap.get(key);
            if(name.equals(o)) {
            action.putValue(Action.ACCELERATOR_KEY, key);
            break;
            }
            }
             */
		}
		editorPane.setActionMap(amap);
		editorPane.setInputMap(JTextComponent.WHEN_FOCUSED, imap);
	}

	private void configActionProperties(Action action, String actionName, String configKey) {

		// if we have an icon, then load it:
		String iconLoc = this.getConfig().getString(configKey + ".SmallIcon", actionName + ".png");
		URL loc = this.getClass().getClassLoader().getResource(DefaultSyntaxAction.SMALL_ICONS_LOC_PREFIX + iconLoc);
		if (loc != null) {
			ImageIcon i = new ImageIcon(loc);
			action.putValue(Action.SMALL_ICON, i);
		}
		// Set the menu text.  Use the Action.NAME property, unless it is
		// already set.
		// The NAME would be set for default actions, and we should not change those names.
		// so we will put another property and use it for the menu text
		String name = this.getProperty(configKey + ".MenuText");
		if (action.getValue(Action.NAME) == null) {
			action.putValue(Action.NAME, name);
		} else {
			action.putValue(ACTION_MENU_TEXT, name);
		}
		// Set the menu tooltips
		String shortDesc = this.getProperty(configKey + ".ToolTip");
		if (shortDesc != null) {
			action.putValue(Action.SHORT_DESCRIPTION, shortDesc);
		} else {
			action.putValue(Action.SHORT_DESCRIPTION, name);
		}
	}

	private SyntaxAction createAction(String actionClassName) {
		SyntaxAction action;
		try {
			Class clazz = Class.forName(actionClassName);
			action = (SyntaxAction) clazz.newInstance();
		} catch (InstantiationException ex) {
			throw new IllegalArgumentException("Cannot create action class: " +
				actionClassName + ". Ensure it has default constructor.", ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException("Cannot create action class: " +
				actionClassName, ex);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Cannot create action class: " +
				actionClassName, ex);
		} catch (ClassCastException ex) {
			throw new IllegalArgumentException("Cannot create action class: " +
				actionClassName, ex);
		}
		return action;
	}

	/**
	 * This is called by Swing to create a Document for the JEditorPane document
	 * This may be called before you actually get a reference to the control.
	 * We use it here to create a proper lexer and pass it to the
	 * SyntaxDocument we return.
	 */
	@Override
	public Document createDefaultDocument() {
		return new SyntaxDocument(this.lexer);
	}

	/**
	 * This is called to initialize the list of <code>Lexer</code>s we have.
	 * You can call  this at initialization, or it will be called when needed.
	 * The method will also add the appropriate EditorKit classes to the
	 * corresponding ContentType of the JEditorPane.  After this is called,
	 * you can simply call the editor.setContentType("text/java") on the
	 * control and you will be done.
	 */
	public synchronized static void initKit() {
		// attempt to find a suitable default font
		String defaultFont = getConfig(DefaultSyntaxKit.class).getString("DefaultFont");
		if (defaultFont != null) {
			DEFAULT_FONT = Font.decode(defaultFont);
		} else {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			String[] fonts = ge.getAvailableFontFamilyNames();
			Arrays.sort(fonts);
			if (Arrays.binarySearch(fonts, "Courier New") >= 0) {
				DEFAULT_FONT = new Font("Courier New", Font.PLAIN, 12);
			} else if (Arrays.binarySearch(fonts, "Courier") >= 0) {
				DEFAULT_FONT = new Font("Courier", Font.PLAIN, 12);
			} else if (Arrays.binarySearch(fonts, "Monospaced") >= 0) {
				DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 13);
			}
		}

		// read the Default Kits and their associated types
		Properties kitsForTypes = JarServiceProvider.readProperties("de/sciss/syntaxpane/kitsfortypes");
		for (Map.Entry e : kitsForTypes.entrySet()) {
			String type = e.getKey().toString();
			String className = e.getValue().toString();
			registerContentType(type, className);
		}
		initialized = true;
	}

	/**
	 * Registers the given content type to use the given class name as its kit
	 * When this is called, an entry is added into the private HashMap of the
	 * registered editors kits.  This is needed so that the SyntaxPane library
	 * has it's own registration of all the EditorKits
	 */
	public static void registerContentType(String type, String className) {
		try {
			// ensure the class is available and that it does supply a no args
			// constructor.  This saves debugging later if the class-name is incorrect
			// or does not behave correctly:
			Class c = Class.forName(className);
			// attempt to create the class, if we cannot with an empty argument
			// then the class is invalid
			Object kit = c.newInstance();
			if (!(kit instanceof EditorKit)) {
				throw new IllegalArgumentException("Cannot register class: " + className +
					". It does not extend EditorKit");
			}
			JEditorPane.registerEditorKitForContentType(type, className);
			CONTENT_TYPES.add(type);
		} catch (InstantiationException ex) {
			throw new IllegalArgumentException("Cannot register class: " + className +
				". Ensure it has Default Constructor.", ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException("Cannot register class: " + className, ex);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Cannot register class: " + className, ex);
		} catch (RuntimeException ex) {
			throw new IllegalArgumentException("Cannot register class: " + className, ex);
		}
	}

	/**
	 * Return all the content types supported by this library.  This will be the
	 * content types in the file WEB-INF/services/resources/de/sciss/syntaxpane/kitsfortypes
	 * @return sorted array of all registered content types
	 */
	public static String[] getContentTypes() {
		String[] types = CONTENT_TYPES.toArray(new String[0]);
		Arrays.sort(types);
		return types;
	}

	/**
	 * Merges the given properties with the configurations for this Object
	 */
	public void setConfig(Properties config) {
		this.getConfig().putAll(config);
	}

	/**
	 * Sets the given property to the given value.  If the kit is not
	 * initialized,  then calls initKit
	 */
	public void setProperty(String key, String value) {
		this.getConfig().put(key, value);
	}

	/**
	 * Return the property with the given key.  If the kit is not
	 * initialized,  then calls initKit
	 * Be careful when changing property as the default property may be used
	 *
	 * @return value for given key
	 */
	public String getProperty(String key) {
		return this.getConfig().getString(key);
	}

	/**
	 * Gets the configuration for this Object
	 */
	public Configuration getConfig() {
		return getConfig(this.getClass());
	}

	/**
	 * Returns the Configurations object for a Kit.  Perform lazy creation of a
	 * Configuration object if nothing is created.
	 */
	public static synchronized Configuration getConfig(Class<? extends DefaultSyntaxKit> kit) {
		if (CONFIGS == null) {
			CONFIGS = new WeakHashMap<Class<? extends DefaultSyntaxKit>, Configuration>();
			Configuration defaultConfig = new Configuration(DefaultSyntaxKit.class);
			loadConfig(defaultConfig, DefaultSyntaxKit.class);
			CONFIGS.put(DefaultSyntaxKit.class, defaultConfig);
		}

		if (CONFIGS.containsKey(kit)) {
			return CONFIGS.get(kit);
		} else {
			// recursive call until we read the Super duper DefaultSyntaxKit
			Class superKit = kit.getSuperclass();
			@SuppressWarnings("unchecked")
			Configuration defaults = getConfig(superKit);
			Configuration mine = new Configuration(kit, defaults);
			loadConfig(mine, kit);
			CONFIGS.put(kit, mine);
			return mine;
		}
	}

	public Map<String, String> getAbbreviations() {
		// if we have not loaded the abbreviations, then load them now:
		if (abbreviations == null) {
			String cl = this.getClass().getName().replace('.', '/').toLowerCase();
			abbreviations = JarServiceProvider.readStringsMap(cl + "/abbreviations.properties");
		}
		return abbreviations;
	}

	/**
	 * Adds an abbreviation to this kit's abbreviations.
	 */
	public static void addAbbreviation(String abbr, String template) {
		if (abbreviations == null) {
			abbreviations = new HashMap<String, String>();
		}
		abbreviations.put(abbr, template);
	}

	/**
	 * Gets the template for the given abbreviation
	 */
	public static String getAbbreviation(String abbr) {
		return abbreviations == null ? null : abbreviations.get(abbr);
	}

	private static void loadConfig(Configuration conf, Class<? extends EditorKit> kit) {
		String url = kit.getName().replace(".", "/") + "/config";
		Properties p = JarServiceProvider.readProperties(url, Locale.getDefault());
		if (p.size() == 0) {
			LOG.log(Level.INFO, "unable to load configuration for: {0} from: {1}.properties",
				new Object[]{kit, url});
		} else {
			conf.putAll(p);
		}
	}

	@Override
	public String getContentType() {
		return "text/" + this.getClass().getSimpleName().replace("SyntaxKit", "").toLowerCase();
	}
}
