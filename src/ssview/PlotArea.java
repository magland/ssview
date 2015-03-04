package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;

/**
 *
 * @author magland
 */
class PlotArea {

	double m_width = 0;
	double m_height = 0;
	double m_xmin = 0, m_xmax = 0;
	double m_ymin = 0, m_ymax = 0;
	Rect m_plot_rect = new Rect(0, 0, 0, 0);
	int m_line_width = 1;
	boolean m_do_incremental_loading = false;
	boolean m_connect_zeros = true;

	List<PlotSeries> m_series = new ArrayList<>();

	public PlotArea() {
	}

	public void setSize(double W, double H) {
		m_width = W;
		m_height = H;
		m_plot_rect = new Rect(0, 0, W, H);
	}

	public void clearSeries() {
		m_series.clear();
	}

	public PlotSeries addSeries(PlotSeries SS) {
		m_series.add(SS);
		return SS;
	}

	public void setXRange(double xmin, double xmax) {
		m_xmin = xmin;
		m_xmax = xmax;
	}

	public void setYRange(double ymin, double ymax) {
		m_ymin = ymin;
		m_ymax = ymax;
	}

	public Vec2 yRange() {
		return new Vec2(m_ymin, m_ymax);
	}

	public void setLineWidth(int w) {
		m_line_width = w;
	}

	public void setDoIncrementalLoading(boolean val) {
		m_do_incremental_loading = val;
	}

	public void setConnectZeros(boolean val) {
		m_connect_zeros = val;
	}

	int[] pix_res_values = {500, 1000, 2000, 4000};
	int current_refresh_pix_res_index = 0;
	int current_refresh_code = 1;
	int pix_res_index = 0;

	public void refresh(GraphicsContext gc) {
		gc.clearRect(0, 0, m_width, m_height);
		current_refresh_code++;
		do_refresh(current_refresh_code, 0, m_xmin - 1, gc);
	}

	void do_refresh(int code, int pr_index, double xmin, GraphicsContext gc) {
		long starttime0 = System.nanoTime();

		if (code != current_refresh_code) {
			return;
		}
		int pr = -1;
		if (m_do_incremental_loading) {
			if (pr_index >= pix_res_values.length) {
				return;
			}
			pr = pix_res_values[pr_index];
		}

		int tot_num_points = (int) (m_xmax - m_xmin + 1);

		int incr = 1;
		if ((m_do_incremental_loading) && (tot_num_points > pr)) {
			incr = (int) Math.ceil(tot_num_points * 1.0 / pr);
		}
		double tmpx1 = coordToPix(new Vec2(xmin, 0)).x;
		double tmpx2 = coordToPix(new Vec2(xmin + tot_num_points, 0)).x + 1;

		int num_points_per_run = 1000 * 1000;
		double xmax = xmin + num_points_per_run * 1.0 / m_series.size() * incr;
		//double xmax=xmin+(tmpx2-tmpx1)/tot_num_points*incr*num_points_per_run/m_series.size();
		Vec2 PP0 = coordToPix(new Vec2(xmin, m_ymin));
		Vec2 PP1 = coordToPix(new Vec2(xmax, m_ymax));
		/*
		 String[] color_strings={
		 "#F7977A","#FDC68A",
		 "#C4DF9B","#82CA9D",
		 "#6ECFF6","#8493CA",
		 "#A187BE","#F49AC2",
		 "#F9AD81","#FFF79A",
		 "#A2D39C","#7BCDC8",
		 "#7EA7D8","#8882BE",
		 "#BC8DBF","#F6989D"
		 };
		 gc.setFill(Color.web(color_strings[((int)Math.abs(xmin)) % color_strings.length]));
		 gc.fillRect(Math.min(PP0.x,PP1.x),Math.min(PP0.y,PP1.y), Math.abs(PP0.x-PP1.x), Math.abs(PP0.y-PP1.y));
		 */
		gc.clearRect(Math.min(PP0.x, PP1.x), Math.min(PP0.y, PP1.y), Math.abs(PP0.x - PP1.x), Math.abs(PP0.y - PP1.y));

		for (int ss = 0; ss < m_series.size(); ss++) {
			PlotSeries SS = m_series.get(ss);
			int N = SS.xvals.totalSize();
			boolean is_first = true;
			gc.beginPath();
			gc.setStroke(SS.color);
			gc.setLineWidth(m_line_width);
			int i1 = (int) (xmin - incr * 10 - m_xmin);
			int i2 = (int) (xmax + incr * 10 - m_xmin);
			i1 = (i1 / incr - 1) * incr;
			i1 = (int) Math.min(Math.max(i1, 0), N);
			i2 = (i2 / incr + 1) * incr;
			i2 = (int) Math.min(Math.max(i2, 0), N);
			double last_val = 0;
			for (int i = i1; i < i2; i += incr) {
				double x0 = SS.xvals.value(0, i);
				if (!m_do_incremental_loading) {
					double val = SS.yvals.value(0, i);
					if (val == Double.POSITIVE_INFINITY) {
						is_first = true;
					} else {
						Vec2 pix1 = coordToPix(new Vec2(x0, val + SS.offset));
						if ((val == 0) && (last_val == 0)) {
							gc.moveTo(pix1.x, pix1.y);
							is_first = false;
						} else {
							if (is_first) {
								is_first = false;
								gc.moveTo(pix1.x, pix1.y);
							} else {
								gc.lineTo(pix1.x, pix1.y);
							}
						}
					}
					last_val = val;
				} else { //incremental loading
					double minval = Double.POSITIVE_INFINITY;
					double maxval = Double.NEGATIVE_INFINITY;
					int incr2 = 1;
					for (int j = 0; (j < incr) && (i + j < N); j += incr2) {
						double val = SS.yvals.value(0, i + j);
						if ((val != Double.POSITIVE_INFINITY) && (val != Double.NEGATIVE_INFINITY)) {
							if (val < minval) {
								minval = val;
							}
							if (val > maxval) {
								maxval = val;
							}
						}
					}
					double y1 = minval;
					double y2 = maxval;
					Vec2 pix1 = coordToPix(new Vec2(x0, y1 + SS.offset));
					Vec2 pix2 = coordToPix(new Vec2(x0, y2 + SS.offset));
					if ((minval == 0) && (maxval == 0) && (last_val == 0)) {
						gc.moveTo(pix1.x, pix1.y);
						is_first = false;
					} else {
						if (minval == Double.POSITIVE_INFINITY) {
							is_first = true;
						} else {

							if (is_first) {
								is_first = false;
								gc.moveTo(pix1.x, pix1.y);
							} else {
								gc.lineTo(pix1.x, pix1.y);
							}
							if (y2 != y1) {
								gc.lineTo(pix2.x, pix2.y);
							}
						}
					}
					last_val = maxval;
				}
			}
			gc.stroke();
		}
		double elapsed = (System.nanoTime() - starttime0) * 1.0 / (1000 * 1000);
		if (m_do_incremental_loading) {
			if (xmax < m_xmax) {
				CallbackHandler.scheduleCallback(() -> {
					do_refresh(code, pr_index, xmax, gc);
				}, (int) elapsed + 50);
			} else {
				if ((incr > 1) && (pr_index + 1 < pix_res_values.length)) {
					CallbackHandler.scheduleCallback(() -> {
						do_refresh(code, pr_index + 1, m_xmin - 1, gc);
					}, (int) elapsed + 400); //add the 400 to give it some delay to enhance user experience
				}
			}
		}
	}

	public Vec2 coordToPix(Vec2 coord) {
		if (m_xmax <= m_xmin) {
			return new Vec2(0, 0);
		}
		if (m_ymax <= m_ymin) {
			return new Vec2(0, 0);
		}
		double x0 = coord.x;
		double y0 = coord.y;
		double pctx = (x0 - m_xmin) / (m_xmax - m_xmin);
		double pcty = (y0 - m_ymin) / (m_ymax - m_ymin);
		double x1 = m_plot_rect.x + m_plot_rect.w * pctx;
		double y1 = m_plot_rect.y + m_plot_rect.h * (1 - pcty);
		return new Vec2(x1, y1);
	}

	public Vec2 pixToCoord(Vec2 pix) {
		if (m_plot_rect.w <= 0) {
			return new Vec2(0, 0);
		}
		if (m_plot_rect.h <= 0) {
			return new Vec2(0, 0);
		}
		double x0 = pix.x;
		double y0 = pix.y;
		double pctx = (x0 - m_plot_rect.x) / m_plot_rect.w;
		double pcty = (y0 - m_plot_rect.y) / m_plot_rect.h;
		double x1 = m_xmin + (m_xmax - m_xmin) * pctx;
		double y1 = m_ymax - (m_ymax - m_ymin) * pcty;
		return new Vec2(x1, y1);
	}
}

class PlotSeries {

	public Mda xvals = new Mda();
	public Mda yvals = new Mda();
	public Color color = Color.BLACK;
	public double offset = 0;

	public PlotSeries(Mda xvals0, Mda yvals0, Color color0, double offset0) {
		xvals = xvals0;
		yvals = yvals0;
		color = color0;
		offset = offset0;
	}

}

class Vec2 {

	public double x = 0, y = 0;

	public Vec2(double x0, double y0) {
		x = x0;
		y = y0;
	}
}

class Rect {

	public double x, y, w, h;

	public Rect(double x0, double y0, double w0, double h0) {
		x = x0;
		y = y0;
		w = w0;
		h = h0;
	}
}

class ViewColors {

	static public Color[] getViewColors(int num) {
		if (num == 1) {
			String[] color_strings = {
				"#F7977A", "#FDC68A",
				"#C4DF9B", "#82CA9D",
				"#6ECFF6", "#8493CA",
				"#A187BE", "#F49AC2",
				"#F9AD81", "#FFF79A",
				"#A2D39C", "#7BCDC8",
				"#7EA7D8", "#8882BE",
				"#BC8DBF", "#F6989D"
			};

			Color[] view_colors = new Color[color_strings.length];
			for (int i = 0; i < color_strings.length; i++) {
				view_colors[i] = Color.web(color_strings[i]);
			}

			return view_colors;
		} else {
			Color[] ret = {
				Color.DARKBLUE, Color.DARKGREEN,
				Color.DARKRED, Color.DARKCYAN,
				Color.DARKMAGENTA, Color.DARKORANGE,
				Color.BLACK
			};
			return ret;
		}
	}
}
