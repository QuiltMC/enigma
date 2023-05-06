package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.DeobfuscatedClassesDocker;
import cuchaz.enigma.gui.docker.ObfuscatedClassesDocker;
import cuchaz.enigma.gui.util.AbstractListCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.search.SearchEntry;
import cuchaz.enigma.gui.search.SearchUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SearchDialog {
	private final JTextField searchField;
	private final JCheckBox classesCheckBox;
	private final JCheckBox methodsCheckBox;
	private final JCheckBox fieldsCheckBox;
	private DefaultListModel<SearchEntryImpl> classListModel;
	private final JList<SearchEntryImpl> classList;
	private final JDialog dialog;

	private final Gui parent;
	private final SearchUtil<SearchEntryImpl> util;
	private final List<Type> searchedTypes = new ArrayList<>();
	private SearchUtil.SearchControl currentSearch;

	public SearchDialog(Gui parent) {
		this.parent = parent;

		this.util = new SearchUtil<>();

		this.dialog = new JDialog(parent.getFrame(), I18n.translate("menu.search"), true);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(ScaleUtil.createEmptyBorder(4, 4, 4, 4));
		contentPane.setLayout(new BorderLayout(ScaleUtil.scale(4), ScaleUtil.scale(4)));

		this.searchField = new JTextField();
		this.searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				SearchDialog.this.updateList();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				SearchDialog.this.updateList();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				SearchDialog.this.updateList();
			}
		});
		this.searchField.addKeyListener(GuiUtil.onKeyPress(this::onKeyPressed));
		this.searchField.addActionListener(e -> this.openSelected());

		JPanel enabledTypes = new JPanel();
		enabledTypes.setLayout(new FlowLayout(FlowLayout.CENTER));

		this.classesCheckBox = new JCheckBox(I18n.translate("prompt.search.classes"));
		this.classesCheckBox.addMouseListener(this.createCheckboxListener(Type.CLASS));

		this.methodsCheckBox = new JCheckBox(I18n.translate("prompt.search.methods"));
		this.methodsCheckBox.addMouseListener(this.createCheckboxListener(Type.METHOD));

		this.fieldsCheckBox = new JCheckBox(I18n.translate("prompt.search.fields"));
		this.fieldsCheckBox.addMouseListener(this.createCheckboxListener(Type.FIELD));

		enabledTypes.add(this.classesCheckBox);
		enabledTypes.add(this.methodsCheckBox);
		enabledTypes.add(this.fieldsCheckBox);

		JPanel topBar = new JPanel();
		topBar.setLayout(new BorderLayout());
		topBar.add(enabledTypes, BorderLayout.SOUTH);
		topBar.add(this.searchField, BorderLayout.NORTH);

		contentPane.add(topBar, BorderLayout.NORTH);

		this.classListModel = new DefaultListModel<>();
		this.classList = new JList<>();
		this.classList.setModel(this.classListModel);
		this.classList.setCellRenderer(new ListCellRendererImpl(parent));
		this.classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.classList.addMouseListener(GuiUtil.onMouseClick(mouseEvent -> {
			if (mouseEvent.getClickCount() >= 2) {
				int idx = this.classList.locationToIndex(mouseEvent.getPoint());
				SearchEntryImpl entry = this.classList.getModel().getElementAt(idx);
				this.openEntry(entry);
			}
		}));
		contentPane.add(new JScrollPane(this.classList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton open = new JButton(I18n.translate("prompt.open"));
		open.addActionListener(event -> this.openSelected());
		buttonBar.add(open);
		JButton cancel = new JButton(I18n.translate("prompt.cancel"));
		cancel.addActionListener(event -> this.close());
		buttonBar.add(cancel);
		contentPane.add(buttonBar, BorderLayout.SOUTH);

		// apparently the class list doesn't update by itself when the list
		// state changes and the dialog is hidden
		this.dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				SearchDialog.this.classList.updateUI();
			}
		});

		this.dialog.setContentPane(contentPane);
		this.dialog.setSize(ScaleUtil.getDimension(400, 500));
		this.dialog.setLocationRelativeTo(parent.getFrame());
	}

	private MouseListener createCheckboxListener(Type type) {
		return GuiUtil.onMouseClick(e -> {
			if (SearchDialog.this.getCheckBox(type).isSelected() && !SearchDialog.this.searchedTypes.contains(type)) {
				SearchDialog.this.show(false, type);
			} else {
				SearchDialog.this.searchedTypes.remove(type);
				SearchDialog.this.show(false);
			}
		});
	}

	private JCheckBox getCheckBox(Type type) {
		return switch (type) {
			case CLASS -> this.classesCheckBox;
			case METHOD -> this.methodsCheckBox;
			case FIELD -> this.fieldsCheckBox;
		};
	}

	private void clearCheckBoxes() {
		for (Type type : Type.values()) {
			this.getCheckBox(type).setSelected(false);
		}
	}

	public void show(Type type) {
		this.show(true, type);
	}

	/**
	 * Shows the search dialog, displaying the provided types.
	 * @param types the search types to show
	 * @param clear whether to clear previously saved types. If false with no types, will add all types.
	 */
	public void show(boolean clear, Type... types) {
		this.util.clear();
		if (clear) {
			this.searchedTypes.clear();
			this.clearCheckBoxes();
		} else {
			// if no types are provided, add all types to avoid showing an empty dialog
			if (this.searchedTypes.isEmpty()) {
				this.searchedTypes.addAll(Arrays.asList(Type.values()));
			}
		}

		this.searchedTypes.addAll(Arrays.asList(types));

		final EntryIndex entryIndex = this.parent.getController().project.getJarIndex().getEntryIndex();

		for (Type searchedType : this.searchedTypes) {
			this.getCheckBox(searchedType).setSelected(true);

			switch (searchedType) {
				case CLASS -> entryIndex.getClasses().parallelStream()
						.filter(e -> !e.isInnerClass())
						.map(e -> SearchEntryImpl.from(e, this.parent.getController()))
						.map(SearchUtil.Entry::from)
						.sequential()
						.forEach(this.util::add);
				case METHOD -> entryIndex.getMethods().parallelStream()
						.filter(e -> !e.isConstructor() && !entryIndex.getMethodAccess(e).isSynthetic())
						.map(e -> SearchEntryImpl.from(e, this.parent.getController()))
						.map(SearchUtil.Entry::from)
						.sequential()
						.forEach(this.util::add);
				case FIELD -> entryIndex.getFields().parallelStream()
						.map(e -> SearchEntryImpl.from(e, this.parent.getController()))
						.map(SearchUtil.Entry::from)
						.sequential()
						.forEach(this.util::add);
			}
		}

		this.updateList();

		this.searchField.requestFocus();
		this.searchField.selectAll();

		this.dialog.setVisible(true);
	}

	private void openSelected() {
		SearchEntryImpl selectedValue = this.classList.getSelectedValue();
		if (selectedValue != null) {
			this.openEntry(selectedValue);
		}
	}

	private void openEntry(SearchEntryImpl entryImpl) {
		this.close();
		this.util.hit(entryImpl);
		this.parent.getController().navigateTo(entryImpl.obf);
		if (entryImpl.obf instanceof ClassEntry entry) {
			if (entryImpl.deobf != null) {
				DeobfuscatedClassesDocker deobfuscatedPanel = Docker.getDocker(DeobfuscatedClassesDocker.class);
				deobfuscatedPanel.getClassSelector().setSelectionClass((ClassEntry) entryImpl.deobf);
			} else {
				ObfuscatedClassesDocker obfuscatedPanel = Docker.getDocker(ObfuscatedClassesDocker.class);
				obfuscatedPanel.getClassSelector().setSelectionClass(entry);
			}
		} else {
			if (entryImpl.deobf != null && entryImpl.deobf.getParent() != null) {
				DeobfuscatedClassesDocker deobfuscatedPanel = Docker.getDocker(DeobfuscatedClassesDocker.class);
				deobfuscatedPanel.getClassSelector().setSelectionClass((ClassEntry) entryImpl.deobf.getParent());
			} else if (entryImpl.obf.getParent() != null) {
				ObfuscatedClassesDocker obfuscatedPanel = Docker.getDocker(ObfuscatedClassesDocker.class);
				obfuscatedPanel.getClassSelector().setSelectionClass((ClassEntry) entryImpl.obf.getParent());
			}
		}
	}

	private void close() {
		this.dialog.setVisible(false);
	}

	// Updates the list of class names
	private void updateList() {
		if (this.currentSearch != null) this.currentSearch.stop();

		DefaultListModel<SearchEntryImpl> updatedClassListModel = new DefaultListModel<>();
		this.classListModel = updatedClassListModel;
		this.classList.setModel(updatedClassListModel);

		// handle these search result like minecraft scheduled tasks to prevent
		// flooding swing buttons inputs etc with tons of (possibly outdated) invocations
		record Order(int idx, SearchEntryImpl e) {}
		Queue<Order> queue = new ConcurrentLinkedQueue<>();
		Runnable updater = new Runnable() {
			@Override
			public void run() {
				if (SearchDialog.this.classListModel != updatedClassListModel || !SearchDialog.this.dialog.isVisible()) {
					return;
				}

				// too large count may increase delay for key and input handling, etc.
				int count = 100;
				while (count > 0 && !queue.isEmpty()) {
					var o = queue.remove();
					updatedClassListModel.insertElementAt(o.e, o.idx);
					count--;
				}

				SwingUtilities.invokeLater(this);
			}
		};

		this.currentSearch = this.util.asyncSearch(this.searchField.getText(), (idx, e) -> queue.add(new Order(idx, e)));
		SwingUtilities.invokeLater(updater);
	}

	public void dispose() {
		this.dialog.dispose();
	}

	private void onKeyPressed(KeyEvent e) {
		if (KeyBinds.SEARCH_DIALOG_NEXT.matches(e)) {
			int next = this.classList.isSelectionEmpty() ? 0 : this.classList.getSelectedIndex() + 1;
			this.classList.setSelectedIndex(next);
			this.classList.ensureIndexIsVisible(next);
		} else if (KeyBinds.SEARCH_DIALOG_PREVIOUS.matches(e)) {
			int prev = this.classList.isSelectionEmpty() ? this.classList.getModel().getSize() : this.classList.getSelectedIndex() - 1;
			this.classList.setSelectedIndex(prev);
			this.classList.ensureIndexIsVisible(prev);
		} else if (KeyBinds.EXIT.matches(e)) {
			this.close();
		}
	}

	private record SearchEntryImpl(ParentedEntry<?> obf, ParentedEntry<?> deobf) implements SearchEntry {
		@Override
		public List<String> getSearchableNames() {
			if (this.deobf != null) {
				return Arrays.asList(this.obf.getSimpleName(), this.deobf.getSimpleName());
			} else {
				return Collections.singletonList(this.obf.getSimpleName());
			}
		}

		@Override
		public String getIdentifier() {
			return this.obf.getFullName();
		}

		@Override
		public Type getType() {
			return Type.get(this.obf);
		}

		@Override
		public String toString() {
			return String.format("SearchEntryImpl { obf: %s, deobf: %s }", this.obf, this.deobf);
		}

		public static SearchEntryImpl from(ParentedEntry<?> e, GuiController controller) {
			ParentedEntry<?> deobf = controller.project.getMapper().deobfuscate(e);
			if (deobf.equals(e)) deobf = null;
			return new SearchEntryImpl(e, deobf);
		}
	}

	private static final class ListCellRendererImpl extends AbstractListCellRenderer<SearchEntryImpl> {
		private final Gui gui;
		private final JLabel mainName;
		private final JLabel secondaryName;

		ListCellRendererImpl(Gui gui) {
			this.setLayout(new BorderLayout());
			this.gui = gui;

			this.mainName = new JLabel();
			this.add(this.mainName, BorderLayout.WEST);

			this.secondaryName = new JLabel();
			this.secondaryName.setFont(this.secondaryName.getFont().deriveFont(Font.ITALIC));
			this.secondaryName.setForeground(Color.GRAY);
			this.add(this.secondaryName, BorderLayout.EAST);
		}

		@Override
		public void updateUiForEntry(JList<? extends SearchEntryImpl> list, SearchEntryImpl value, int index, boolean isSelected, boolean cellHasFocus) {
			if (value.deobf == null) {
				this.mainName.setText(value.obf.getContextualName());
				this.mainName.setToolTipText(value.obf.getFullName());
				this.secondaryName.setText("");
				this.secondaryName.setToolTipText("");
			} else {
				this.mainName.setText(value.deobf.getContextualName());
				this.mainName.setToolTipText(value.deobf.getFullName());
				this.secondaryName.setText(value.obf.getSimpleName());
				this.secondaryName.setToolTipText(value.obf.getFullName());
			}

			if (value.obf instanceof ClassEntry classEntry) {
				this.mainName.setIcon(GuiUtil.getClassIcon(this.gui, classEntry));
			} else if (value.obf instanceof MethodEntry methodEntry) {
				this.mainName.setIcon(GuiUtil.getMethodIcon(methodEntry));
			} else if (value.obf instanceof FieldEntry) {
				this.mainName.setIcon(GuiUtil.FIELD_ICON);
			}
		}
	}

	/**
	 * Contains all searchable types. Ordered by priority in the search dialog (i.e. the first type here will show up above the second type, etc.).
	 */
	public enum Type {
		CLASS,
		METHOD,
		FIELD;

		public static Type get(Entry<?> entry) {
			if (entry instanceof ClassEntry) {
				return CLASS;
			} else if (entry instanceof MethodEntry) {
				return METHOD;
			} else if (entry instanceof FieldEntry) {
				return FIELD;
			} else {
				throw new IllegalArgumentException("Non-searchable entry type: " + entry);
			}
		}
	}
}
