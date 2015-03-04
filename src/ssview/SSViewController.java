/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssview;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jviewmda.Mda;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class SSViewController {

	public SSViewController() {

	}

	public void showTimeSeries(String fname) {
		showTimeSeries(fname, "", "", "");
	}

	public void showTimeSeries(String fname, String labels_fname) {
		showTimeSeries(fname, labels_fname, "", "");
	}

	public void showTimeSeries(String fname, String labels_fname, String labels2_fname) {
		showTimeSeries(fname, labels_fname, labels2_fname, "");
	}

	public SSViewMainWindow createMainWindow() {
		Stage stage = new Stage();

		SSViewMainWindow WW = new SSViewMainWindow();

		StackPane root = new StackPane();
		root.getChildren().add(WW);

		Scene scene = new Scene(root, 1000, 500);

		stage.setTitle("SSView");
		stage.setScene(scene);
		stage.show();

		return WW;
	}

	public SSTimeSeriesWidget createTimeSeriesWidget() {
		return createTimeSeriesWidget("default");
	}

	public SSTimeSeriesWidget createTimeSeriesWidget(String widget_type) {
		/*Stage stage = new Stage();

		 SSTimeSeriesWidget WW = new SSTimeSeriesWidget();

		 StackPane root = new StackPane();
		 root.getChildren().add(WW);

		 Scene scene = new Scene(root, 1000, 500);

		 stage.setTitle("SSView");
		 stage.setScene(scene);
		 stage.show();
		
		 return WW;*/
		return new SSTimeSeriesWidget(widget_type);
	}

	public Mda readMda(String fname) {
		Mda ret = new Mda();
		ret.read(fname);
		return ret;
	}

	public void showTimeSeries(String fname, String labels_fname, String labels2_fname, String labels3_fname) {
		Stage stage = new Stage();

		SSTimeSeriesWidget WW = new SSTimeSeriesWidget();

		StackPane root = new StackPane();
		root.getChildren().add(WW);

		Scene scene = new Scene(root, 1000, 500);

		stage.setTitle("SSView");
		stage.setScene(scene);
		stage.show();

		Mda LL = null;
		if (!labels_fname.isEmpty()) {
			LL = new Mda();
			if (!LL.read(labels_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels_fname);
				return;
			}
		}

		Mda X = new Mda();
		if (!X.read(fname)) {
			JUtils.showError("Unable to read", "Unable to load array: " + fname);
			return;
		}
		if (can_be_label_array(LL)) {
			WW.addDataArray(X, LL);
		} else {
			WW.addDataArray(X, null);
		}

		if (LL != null) {
			WW.addDataArray(LL, null);
		}

		if (!labels2_fname.isEmpty()) {
			Mda Y = new Mda();
			if (!Y.read(labels2_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels2_fname);
				return;
			}
			WW.addDataArray(Y, null);

		}

		if (!labels3_fname.isEmpty()) {
			Mda Y = new Mda();
			if (!Y.read(labels3_fname)) {
				JUtils.showError("Unable to read", "Unable to load array: " + labels3_fname);
				return;
			}
			WW.addDataArray(Y, null);
		}

	}

	boolean can_be_label_array(Mda L) {
		if (L == null) {
			return false;
		}
		long num_zeros = 0;
		long tot = 0;
		int NN = L.totalSize();
		for (int i = 0; i < NN; i += 100) {
			if (L.value1(i) == 0) {
				num_zeros++;
			}
			tot++;
		}
		if (tot > 0) {
			if (num_zeros > tot * 0.5) {
				return true;
			}
		}
		return false;
	}
}
