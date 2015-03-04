package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.SplitPane;

/**
 *
 * @author magland
 */
public class SSViewMainWindow extends SplitPane {

	List<SSTimeSeriesWidget> m_time_series_widgets = new ArrayList<>();

	public SSViewMainWindow() {

	}

	public void remove(SSTimeSeriesWidget WW) {
		this.getItems().remove(WW);
		if (this.getItems().size() == 0) {
			if ((this.getScene() != null) && (this.getScene().getWindow() != null)) {
				this.getScene().getWindow().hide();
			}
			return;
		}
		do_resize();
	}

	public void add(SSTimeSeriesWidget WW) {
		this.getItems().add(WW);
		m_time_series_widgets.add(WW);

		if (m_time_series_widgets.size() > 1) {
			connect_time_series_widgets(WW, m_time_series_widgets.get(0));
		}
		WW.setParentWindow(this);

		do_resize();
	}

	void do_resize() {
		int len = this.getDividerPositions().length;
		for (int i = 0; i < len; i++) {
			this.setDividerPosition(i, (i + 1) * 1.0 / (len + 1));
		}
	}

	////////////// PRIVATE ///////////////////////////////////
	void connect_time_series_widgets(SSTimeSeriesWidget W1, SSTimeSeriesWidget W2) {
		do_connect_tsw(W1, W2);
		do_connect_tsw(W2, W1);
	}

	void do_connect_tsw(SSTimeSeriesWidget W1, SSTimeSeriesWidget W2) {
		W1.onCurrentTimepointChanged(() -> {
			W2.setCurrentTimepoint(W1.currentTimepoint());
		});
	}
}
