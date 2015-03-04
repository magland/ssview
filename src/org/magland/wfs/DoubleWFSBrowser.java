package org.magland.wfs;

import javafx.scene.layout.HBox;

/**
 *
 * @author magland
 */
public class DoubleWFSBrowser extends HBox {

	WFSBrowser m_left_browser = new WFSBrowser();
	WFSBrowser m_right_browser = new WFSBrowser();
	String m_base_path = "";
	WFSClient m_client = null;

	public DoubleWFSBrowser() {

	}

	public void setLeftBrowser(WFSBrowser B) {
		m_left_browser = B;
	}

	public void setRightBrowser(WFSBrowser B) {
		m_right_browser = B;
	}

	public void setClient(WFSClient CC) {
		m_client = CC;
		m_left_browser.setClient(CC);
		m_right_browser.setClient(CC);
	}

	public void setBasePath(String path) {
		m_base_path = path;
	}

	public void addOpenFileHandler(OpenFileHandler handler) {
		m_left_browser.addOpenFileHandler(handler);
		m_right_browser.addOpenFileHandler(handler);
	}

	public void initialize() {

		this.getChildren().removeAll(this.getChildren());
		this.getChildren().addAll(m_left_browser, m_right_browser);

		m_right_browser.setPrefWidth(Integer.MAX_VALUE);
		m_right_browser.setPrefHeight(Integer.MAX_VALUE);

		m_left_browser.setPrefWidth(250);
		m_left_browser.setPrefHeight(Integer.MAX_VALUE);

		m_left_browser.setBasePath(m_base_path);
		m_right_browser.setBasePath(m_base_path);
		m_left_browser.setShowFiles(false);
		m_right_browser.setFoldersExpandable(false);
		m_left_browser.onSelectedItemChanged(() -> {
			String path = m_left_browser.selectedItemPath();
			m_right_browser.setBasePath(path);
			m_right_browser.refresh();
		});
		m_right_browser.onFolderActivated(() -> {
			String path = m_right_browser.selectedItemPath();
			m_right_browser.setBasePath(path);
			m_right_browser.refresh();
		});

		m_left_browser.initialize();
		m_right_browser.initialize();

		refresh_layout();
		this.widthProperty().addListener((tmp1) -> {
			refresh_layout();
		});
	}

	private void refresh_layout() {
		double W0 = this.getWidth();
		if (W0 < 600) {
			m_left_browser.setMinWidth(W0 / 2);
		} else {
			m_left_browser.setMinWidth(300 + (W0 - 600) * 0.25);
		}
	}

}
