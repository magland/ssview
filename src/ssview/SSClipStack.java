package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
public class SSClipStack extends ExpandingCanvas {

	PlotArea m_plot_area = new PlotArea();
	Mda m_clip_data = new Mda();
	double[] m_minvals = new double[1];
	double[] m_maxvals = new double[1];
	double[] m_plot_offsets = new double[0];
	double[] m_plot_y1 = new double[0];
	double[] m_plot_y2 = new double[0];
	boolean m_show_plots = true;
	Color[] m_channel_colors = ViewColors.getViewColors(2);
	CallbackHandler CH = new CallbackHandler();

	public SSClipStack() {
		this.setOnRefresh(() -> {
			Platform.runLater(() -> {
				refresh_plot();
				CH.trigger("refresh-required", true);
			});
		});
	}

	public void setClipData(Mda data) {
		m_clip_data = data;

		set_data_2();
	}

	//////////////////////////// PRIVATE ////////////////////////////////
	void set_data_2() {
		int M = m_clip_data.size(0);
		int T = m_clip_data.size(1);
		int N = m_clip_data.size(2);

		m_minvals = new double[M];
		m_maxvals = new double[M];
		if (N > 1) {
			for (int ch = 0; ch < M; ch++) {
				for (int tt = 0; tt < T; tt++) {
					for (int i = 0; i < N; i++) {
						double val = m_clip_data.value(ch, tt, i);
						if (val < m_minvals[ch]) {
							m_minvals[ch] = val;
						}
						if (val > m_maxvals[ch]) {
							m_maxvals[ch] = val;
						}
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

	void refresh_plot() {
		int M = m_clip_data.size(0);
		int T = m_clip_data.size(1);
		int N = m_clip_data.size(2);

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

		int xrange_min = 0;
		int xrange_max = T - 1;

		m_plot_area.setXRange(xrange_min - 1, xrange_max + 1);
		m_plot_area.setYRange(m_plot_offsets[0] + m_minvals[0] - max00 / 20, m_plot_offsets[M - 1] + m_maxvals[M - 1] + max00 / 20);

		{
			for (int ch = 0; ch < M; ch++) {
				for (int cc = 0; cc < N; cc++) {
					Mda xvals = new Mda();
					xvals.allocate(1, xrange_max - xrange_min + 1);
					for (int x = xrange_min; x <= xrange_max; x++) {
						xvals.setValue(x, 0, x - xrange_min);
					}
					Mda yvals = new Mda();
					yvals.allocate(1, xrange_max - xrange_min + 1);
					for (int ii = xrange_min; ii <= xrange_max; ii++) {
						double val = m_clip_data.value(ch, ii, cc);
						yvals.setValue(val, 0, ii - xrange_min);
					}
					Color color = get_channel_color(ch);
					m_plot_area.addSeries(new PlotSeries(xvals, yvals, color, m_plot_offsets[ch]));
				}
			}
		}
		m_plot_area.refresh(gc);
	}

	Color get_channel_color(int ch) {
		return m_channel_colors[ch % m_channel_colors.length];
	}
}
