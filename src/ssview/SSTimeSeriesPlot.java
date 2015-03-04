package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;
import ssview.PlotArea;
import ssview.PlotSeries;
import ssview.Rect;
import ssview.Vec2;
import ssview.ViewColors;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author magland
 */
class SSTimeSeriesPlot extends ExpandingCanvas {

	//this
	Mda m_data = new Mda();
	//or this
	Mda m_timepoints_labels = new Mda();

	int m_max_timepoint = 0;
	int m_num_channels = 0;
	PlotArea m_plot_area = new PlotArea();
	double[] m_minvals = new double[1];
	double[] m_maxvals = new double[1];
	double[] m_plot_offsets = new double[0];
	double[] m_plot_y1 = new double[0];
	double[] m_plot_y2 = new double[0];
	int m_xrange_min = -1;
	int m_xrange_max = -1;
	boolean m_show_plots = true;
	Color[] m_channel_colors = ViewColors.getViewColors(2);
	CallbackHandler CH = new CallbackHandler();

	public SSTimeSeriesPlot() {
		this.setOnRefresh(() -> {
			Platform.runLater(() -> {
				refresh_plot();
				CH.trigger("refresh-required", true);
			});
		});
	}

	public void onRefreshRequired(Runnable X) {
		CH.bind("refresh-required", X);
	}

	public void setData(Mda X) {
		m_data = X;
		m_max_timepoint = m_data.size(1) - 1;
		m_num_channels = m_data.size(0);
		m_plot_area.setDoIncrementalLoading(true);

		set_data_2();

		m_plot_area.setConnectZeros(false);
	}

	public void setTimepointsLabels(Mda TL) {
		m_timepoints_labels = TL;
		m_max_timepoint = 0;
		m_num_channels = 0;
		for (int i = 0; i < TL.size(1); i++) {
			if ((int) (TL.value(1, i) - 1 + 1) > m_num_channels) { //minus 1 for a zero-based indexing
				m_num_channels = (int) (TL.value(1, i) - 1 + 1); //minus 1 for a zero-based indexing
			}
			if ((int) TL.value(0, i) - 1 > m_max_timepoint) { //minus 1 for a zero-based indexing
				m_max_timepoint = (int) (TL.value(0, i) - 1); //minus 1 for a zero-based indexing
			}
		}

		set_data_2();

		m_plot_area.setConnectZeros(false);
		m_plot_area.setLineWidth(2);
	}

	void set_data_2() {
		int M = m_num_channels;
		int N = m_max_timepoint + 1;

		m_xrange_min = 0;
		m_xrange_max = N - 1;

		m_minvals = new double[M];
		m_maxvals = new double[M];
		if (m_data.size(1) > 1) {
			for (int ch = 0; ch < M; ch++) {
				if (N > 0) {
					m_minvals[ch] = m_data.value(ch, 0);
					m_maxvals[ch] = m_data.value(ch, 0);
				}
				for (int i = 0; i < N; i++) {
					double val = m_data.value(ch, i);
					if (val < m_minvals[ch]) {
						m_minvals[ch] = val;
					}
					if (val > m_maxvals[ch]) {
						m_maxvals[ch] = val;
					}
				}
			}
		} else {
			for (int ch = 0; ch < M; ch++) {
				m_minvals[ch] = 0;
				m_maxvals[ch] = 1;
			}
		}
		refresh_plot();
	}

	public void setShowPlots(boolean val) {
		if (m_show_plots == val) {
			return;
		}
		m_show_plots = val;
		this.refresh_plot();
	}

	public Vec2 coordToPix(Vec2 p) {
		return m_plot_area.coordToPix(p);
	}

	public Vec2 pixToCoord(Vec2 p) {
		return m_plot_area.pixToCoord(p);
	}

	public int pixToChannel(Vec2 p) {
		Vec2 coord = m_plot_area.pixToCoord(p);
		for (int i = 0; i < m_plot_y1.length; i++) {
			if ((coord.y >= m_plot_y1[i]) && (coord.y <= m_plot_y2[i])) {
				return i;
			}
		}
		return -1;
	}

	public Rect channelToRect(int ch) {
		if ((ch < 0) || (ch >= m_plot_y1.length)) {
			return new Rect(-1, -1, -1, -1);
		}
		Vec2 p0 = coordToPix(new Vec2(0, m_plot_y1[ch]));
		Vec2 p1 = coordToPix(new Vec2(m_num_channels, m_plot_y2[ch])); //changed from size(1) to size(0) ... is this right?
		return new Rect(Math.min(p0.x, p1.x), Math.min(p0.y, p1.y), Math.abs(p0.x - p1.x), Math.abs(p0.y - p1.y));
	}

	public Vec2 xRange() {
		return new Vec2(m_xrange_min, m_xrange_max);
	}

	public Vec2 yRange() {
		return m_plot_area.yRange();
	}

	public void setXRange(int xmin, int xmax) {
		m_xrange_min = Math.max(xmin, 0);
		m_xrange_max = Math.min(xmax, m_max_timepoint);
		this.refresh_plot();
	}

	public void setChannelColors(Color[] colors) {
		m_channel_colors = colors.clone();
	}

	////////////////////// PRIVATE /////////////////////////
	void refresh_plot() {
		int M = m_num_channels;
		int NN = m_max_timepoint + 1;

		m_plot_area.setSize(this.getWidth(), this.getHeight());

		m_plot_offsets = new double[M];
		double max00 = 0;
		for (int ch = 0; ch < M; ch++) {
			if (Math.abs(m_minvals[ch]) > max00) {
				max00 = Math.abs(m_minvals[ch]);
			}
			if (Math.abs(m_maxvals[ch]) > max00) {
				max00 = Math.abs(m_maxvals[ch]);
			}
		}
		m_plot_y1 = new double[M];
		m_plot_y2 = new double[M];
		double offset = 0;
		for (int ch = 0; ch < M; ch++) {
			m_plot_y1[ch] = offset;
			offset += (-m_minvals[ch]);
			m_plot_offsets[ch] = offset;
			offset += m_maxvals[ch];
			offset += max00 / 20;
			m_plot_y2[ch] = offset;
		}

		m_plot_area.clearSeries();
		GraphicsContext gc = this.getGraphicsContext2D();
		m_plot_area.setSize(this.getWidth(), this.getHeight());

		if (M == 0) {
			start_refreshing_plot_area(gc);
			return;
		}

		int xrange_min = m_xrange_min;
		int xrange_max = m_xrange_max;

		m_plot_area.setXRange(xrange_min - 1, xrange_max + 1);
		m_plot_area.setYRange(m_plot_offsets[0] + m_minvals[0] - max00 / 20, m_plot_offsets[M - 1] + m_maxvals[M - 1] + max00 / 20);

		if (m_data.size(1) > 1) {
			for (int ch = 0; ch < M; ch++) {
				Mda xvals = new Mda();
				xvals.allocate(1, xrange_max - xrange_min + 1);
				for (int x = xrange_min; x <= xrange_max; x++) {
					xvals.setValue(x, 0, x - xrange_min);
				}
				Mda yvals = new Mda();
				yvals.allocate(1, xrange_max - xrange_min + 1);
				for (int ii = xrange_min; ii <= xrange_max; ii++) {
					double val = m_data.value(ch, ii);
					yvals.setValue(val, 0, ii - xrange_min);
				}
				Color color = get_channel_color(ch);
				m_plot_area.addSeries(new PlotSeries(xvals, yvals, color, m_plot_offsets[ch]));
			}
		} else {
			int[] point_counts = new int[m_num_channels];
			for (int i = 0; i < m_timepoints_labels.size(1); i++) {
				int ch = (int) m_timepoints_labels.value(1, i) - 1; //minus one for a zero-based indexing
				if (ch >= 0) {
					point_counts[ch]++;
				} else {
					for (int j = 0; j < m_num_channels; j++) {
						point_counts[j]++;
					}
				}
			}

			List<int[]> timepoint_series = new ArrayList<>(m_num_channels);
			for (int i = 0; i < m_num_channels; i++) {
				timepoint_series.add(i, new int[point_counts[i]]);
			}
			int[] indices = new int[m_num_channels];
			for (int i = 0; i < m_timepoints_labels.size(1); i++) {
				int ch = (int) m_timepoints_labels.value(1, i) - 1; //minus 1 for a zero-based indexing
				if (ch >= 0) {
					timepoint_series.get(ch)[indices[ch]] = (int) m_timepoints_labels.value(0, i) - 1; //minus 1 for a zero-based indexing
					indices[ch]++;
				} else {
				}
			}

			for (int ch = 0; ch < m_num_channels; ch++) {
				int[] TS = timepoint_series.get(ch);
				int xvals_count = 0;
				int ii = 0;
				while (ii < TS.length) {
					int kk = ii + 1;
					while ((kk < TS.length) && (TS[kk] == TS[ii] + kk - ii)) {
						kk++;
					}
					xvals_count += kk - ii + 2;
					ii = kk;
				}
				Mda xvals = new Mda();
				xvals.allocate(1, xvals_count);
				Mda yvals = new Mda();
				yvals.allocate(1, xvals_count);
				int xvals_ind = 0;
				ii = 0;
				while (ii < TS.length) {
					int kk = ii + 1;
					while ((kk < TS.length) && (TS[kk] == TS[ii] + kk - ii)) {
						kk++;
					}
					xvals.setValue(TS[ii] - 1, 0, xvals_ind);
					yvals.setValue(0, 0, xvals_ind);
					xvals_ind++;
					for (int jj = ii; jj < kk; jj++) {
						xvals.setValue(TS[jj], 0, xvals_ind);
						yvals.setValue(1, 0, xvals_ind);
						xvals_ind++;
					}
					xvals.setValue(TS[kk - 1] + 1, 0, xvals_ind);
					yvals.setValue(0, 0, xvals_ind);
					xvals_ind++;
					ii = kk;
				}

				if (xvals_count > 0) {
					Color color = get_channel_color(ch);
					m_plot_area.addSeries(new PlotSeries(xvals, yvals, color, m_plot_offsets[ch]));
				}
			}
		}
		if (m_show_plots) {
			start_refreshing_plot_area(gc);
		} else {
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}

	void start_refreshing_plot_area(GraphicsContext gc) {
		m_plot_area.refresh(gc);
	}

	double compute_min(Mda X) {
		int N = X.totalSize();
		if (N == 0) {
			return 0;
		}
		double ret = X.value1(0);
		for (int i = 0; i < N; i++) {
			double val = X.value1(i);
			if (val < ret) {
				ret = val;
			}
		}
		return ret;
	}

	double compute_max(Mda X) {
		int N = X.totalSize();
		if (N == 0) {
			return 0;
		}
		double ret = X.value1(0);
		for (int i = 0; i < N; i++) {
			double val = X.value1(i);
			if (val > ret) {
				ret = val;
			}
		}
		return ret;
	}

	Color get_channel_color(int ch) {
		return m_channel_colors[ch % m_channel_colors.length];
	}
}
