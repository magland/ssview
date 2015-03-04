package org.magland.jcommon;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author magland
 */
public class JUtils {

	static public String getFileSuffix(String path) {
		int ind = path.lastIndexOf(".");
		if (ind < 0) {
			return "";
		}
		return path.substring(ind + 1);
	}

	static public String getFileName(String path) {
		int ind = path.lastIndexOf("/");
		if (ind < 0) {
			return path;
		}
		return path.substring(ind + 1);
	}

	static public String getFilePath(String path) {
		int ind = path.lastIndexOf("/");
		if (ind < 0) {
			return "";
		}
		return path.substring(0, ind);
	}

	static public String appendPaths(String path1, String path2) {
		if (path1.isEmpty()) {
			return path2;
		}
		if (path2.isEmpty()) {
			return path1;
		}
		return path1 + "/" + path2;
	}

	static public void showInformation(String title, String message) {
		Dialogs.create()
				.owner(null)
				.title(title)
				.masthead(null)
				.message(message)
				.showInformation();
	}

	static public void showWarning(String title, String message) {
		Dialogs.create()
				.owner(null)
				.title(title)
				.masthead(null)
				.message(message)
				.showWarning();
	}
	static public void showError(String title, String message) {
		Dialogs.create()
				.owner(null)
				.title(title)
				.masthead(null)
				.message(message)
				.showError();
	}

	static public Boolean connectedToInternet() {
		try {
			try {
				URL url = new URL("http://www.google.com");
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.connect();
				if (con.getResponseCode() == 200) {
					return true;
				} else {
					return false;
				}
			} catch (Exception ee) {
				return false;
			}
		} catch (Exception ee) {
			return false;
		}
	}

	static public Stage popupWidgetModal(Node W,String title) {
		return do_popup_widget(W,title,true);
	}
	static public Stage popupWidget(Node W, String title) {
		return do_popup_widget(W,title,false);
	}
	static Stage do_popup_widget(Node W,String title,boolean modal) {
		VBox root = new VBox();
		root.getChildren().addAll(W);

		double W0=W.minWidth(300);
		double H0=W.minHeight(300);
		Scene scene = new Scene(root, W0, H0);

		Stage stage = new Stage();

		if (modal) {
			stage.initModality(Modality.APPLICATION_MODAL);
		}
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();

		return stage;
	}

	static public String createTemporaryDirectory(String name) {
		try {
			String ret = System.getProperty("java.io.tmpdir") + "/" + name;
			try {
				(new File(ret)).mkdir();
			} catch (Exception ee) {

			}
			return ret;
		} catch (Exception ee) {
			return "";
		}
	}
}
