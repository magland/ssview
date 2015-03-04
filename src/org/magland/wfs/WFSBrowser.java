package org.magland.wfs;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;
import org.magland.jcommon.SJO;

/**
 *
 * @author magland
 */
public class WFSBrowser extends TreeView {

	WFSBrowserItem m_root = null;
	WFSClient m_client = null;
	CallbackHandler CH = new CallbackHandler();
	Boolean m_folders_expandable = true;
	String m_base_path = "";
	Boolean m_show_files = true;
	List<OpenFileHandler> m_open_file_handlers = new ArrayList<>();

	public WFSBrowser() {
		this.setShowRoot(false);

		this.getSelectionModel().selectedItemProperty().addListener((obs, oldval, newval) -> {
			TreeItem<WFSBrowserItem> item = (TreeItem<WFSBrowserItem>) newval;
			if (item == null) {
				return;
			}
			CH.trigger("selected-item-changed", true);
		});

		this.setOnMouseClicked(evt -> {
			if (evt.getClickCount() == 2) {
				if (this.selectedItemIsDirectory()) {
					CH.trigger("folder-activated", true);
				} else {
					CH.trigger("item-activated", true);
				}
			}
		});

		this.setOnKeyPressed(evt -> {
			if (evt.getCode() == KeyCode.ENTER) {
				if (this.selectedItemIsDirectory()) {
					CH.trigger("folder-activated", true);
				} else {
					CH.trigger("item-activated", true);
				}
			}
		});

		this.onItemActivated(() -> {
			String path = this.selectedItemPath();
			String suf = JUtils.getFileSuffix(path);
			for (int i = m_open_file_handlers.size() - 1; i >= 0; i--) {
				OpenFileHandler handler = m_open_file_handlers.get(i);
				if (handler.fileTypes().contains(suf)) {
					handler.open(m_client, path);
					return;
				}
			}
		});

		this.addOpenFileHandler(new TextOpenFileHandler());

	}

	public void setClient(WFSClient client) {
		m_client = client;
	}

	public WFSClient client() {
		return m_client;
	}

	public void setFoldersExpandable(Boolean val) {
		m_folders_expandable = val;
	}

	public void setBasePath(String path) {
		m_base_path = path;
	}

	public void setShowFiles(Boolean val) {
		m_show_files = val;
	}

	public void addOpenFileHandler(OpenFileHandler handler) {
		m_open_file_handlers.add(handler);
	}

	public void clearOpenFileHandlers() {
		m_open_file_handlers.clear();
	}

	public void initialize() {
		refresh();
	}

	public void refresh() {
		m_root = new WFSBrowserItem();
		m_root.setPath(m_base_path);
		m_root.setIsDirectory(true);

		this.setRoot(m_root);
	}

	WFSBrowserItem get_selected_item() {
		return (WFSBrowserItem) this.getSelectionModel().getSelectedItem();
	}

	public String selectedItemPath() {
		WFSBrowserItem item = get_selected_item();
		if (item == null) {
			return "";
		}
		return item.getPath();
	}

	public Boolean selectedItemIsDirectory() {
		WFSBrowserItem item = get_selected_item();
		if (item == null) {
			return false;
		}
		return item.isDirectory();
	}

	public void onSelectedItemChanged(Runnable callback) {
		CH.bind("selected-item-changed", callback);
	}

	public void onItemActivated(Runnable callback) {
		CH.bind("item-activated", callback);
	}

	public void onFolderActivated(Runnable callback) {
		CH.bind("folder-activated", callback);
	}

	class WFSBrowserItem extends TreeItem {

		String m_path = "";
		Boolean m_first_get_children = true;
		Boolean m_is_directory = false;
		Boolean m_checked_and_does_not_have_children = false;

		public WFSBrowserItem() {
		}

		public void setPath(String path) {
			m_path = path;
			String str = JUtils.getFileName(path);
			if (str.isEmpty()) {
				str = "ROOT";
			}
			this.setValue(str);
		}

		public String getPath() {
			return m_path;
		}

		@Override
		public ObservableList<WFSBrowserItem> getChildren() {
			if (m_first_get_children) {
				m_first_get_children = false;
				super.getChildren().setAll(build_children());
			}
			return super.getChildren();
		}

		public void setIsDirectory(Boolean val) {
			m_is_directory = val;
		}

		public Boolean isDirectory() {
			return m_is_directory;
		}

		@Override
		public boolean isLeaf() {
			if (m_checked_and_does_not_have_children) {
				return true;
			}
			if (!m_folders_expandable) {
				return (!m_path.equals(m_base_path));
			}
			return !m_is_directory;
		}

		private ObservableList<WFSBrowserItem> build_children() {
			ObservableList<WFSBrowserItem> ret = FXCollections.observableArrayList();
			if (!m_is_directory) {
				return ret;
			}
			if ((!m_folders_expandable) && (!m_path.equals(m_base_path))) {
				return ret;
			}

			SJO tmp1 = m_client.readDirSync(m_path);
			SJO dirs = tmp1.get("dirs");
			int dirslen = dirs.getLength();
			for (int i = 0; i < dirslen; i++) {
				WFSBrowserItem item = new WFSBrowserItem();
				String path0 = JUtils.appendPaths(m_path, dirs.get(i).get("name").toString());
				item.setPath(path0);
				item.setIsDirectory(true);
				ret.add(item);
			}
			if (m_show_files) {
				SJO files = tmp1.get("files");
				int fileslen = files.getLength();
				for (int i = 0; i < fileslen; i++) {
					WFSBrowserItem item = new WFSBrowserItem();
					String path0 = JUtils.appendPaths(m_path, files.get(i).get("name").toString());
					item.setPath(path0);
					item.setIsDirectory(false);
					ret.add(item);
				}
			}

			if (ret.size() == 0) {
				if (!m_checked_and_does_not_have_children) {
					m_checked_and_does_not_have_children = true;
					this.setValue(this.getValue()); //trigger an update of the item
				}
			}

			return ret;
		}
	}

}
