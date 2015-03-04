package ssview;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import jviewmda.ExpandingCanvas;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
class SSTimeSeriesView extends StackPane {

	//this
	Mda m_data = new Mda();
	//or this
	Mda m_timepoints_labels = new Mda();

	int m_max_timepoint = 0;
	int m_num_channels = 0;
	double m_current_x = -1;
	double m_hovered_x = -1;
	int m_control_anchor_x = -1;
	double m_selected_xmin = -1;
	double m_selected_xmax = -1;
	double m_selected_x_anchor = -1;
	int m_current_channel = -1;
	boolean m_cursor_visible = true;
	ExpandingCanvas m_underlay = new ExpandingCanvas();
	ExpandingCanvas m_marker_underlay = new ExpandingCanvas();
	SSTimeSeriesPlot m_plot = new SSTimeSeriesPlot();
	CallbackHandler CH = new CallbackHandler();
	boolean m_can_zoom_all_the_way_out = true;
	List<SSMarker> m_markers = new ArrayList<>();

	public SSTimeSeriesView() {
		this.setStyle("-fx-border-width: 2px;-fx-border-color: brown;");

		this.getChildren().add(m_underlay);
		this.getChildren().add(m_marker_underlay);
		this.getChildren().add(m_plot);
		this.setOnMousePressed(evt -> {
			CH.trigger("clicked", true);
			int x0 = (int) m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY())).x;
			this.setSelectionRange(new Vec2(-1, -1));
			m_selected_x_anchor = x0;

			if (m_current_x != x0) {
				m_current_x = x0;
				refresh_underlay();
				CH.trigger("current-x-changed", true);
			}

			if (evt.getClickCount() == 2) {
				do_translate((int) (x0 - (this.xRange().x + this.xRange().y) / 2));
				return;
			}

			if (evt.isControlDown()) {
				m_control_anchor_x = x0;
			} else if (evt.getButton() == MouseButton.PRIMARY) {
				setCurrentChannel(m_plot.pixToChannel(new Vec2(evt.getX(), evt.getY())));
				refresh_underlay();
			}
		});
		this.setOnMouseReleased(evt -> {
			on_mouse_released(evt);
			m_selected_x_anchor = -1;
			m_control_anchor_x = -1;

		});
		this.setOnMouseDragged(evt -> {
			int x0 = (int) m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY())).x;
			if (evt.isControlDown()) {
				this.getScene().setCursor(Cursor.MOVE);
				if (m_control_anchor_x >= 0) {
					do_translate((int) (m_control_anchor_x - x0));
				}
			} else {
				if ((m_selected_x_anchor >= 0)
						&& (far_enough_away_in_terms_of_pixels(x0, m_selected_x_anchor, MIN_PIX_FOR_ZOOM_SELECTION))) {
					m_selected_xmin = Math.min(m_selected_x_anchor, Math.max(x0, 0));
					m_selected_xmax = Math.max(m_selected_x_anchor, Math.max(x0, 0));
					CH.trigger("selection-range-changed", true);
					refresh_underlay();
				}
			}
		});
		this.setOnMouseMoved(evt -> {
			//this.requestFocus();
			Vec2 hovered_coordinate = m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY()));
			if (m_hovered_x != hovered_coordinate.x) {
				m_hovered_x = hovered_coordinate.x;
				refresh_underlay();
				CH.trigger("hovered-x-changed", false);
			}
		});
		this.setOnMouseExited(evt -> {
			m_hovered_x = -1;
			refresh_underlay();
		});
		this.setOnScroll(evt -> {
			double delta_y = evt.getDeltaY();
			if (delta_y > 0) {
				schedule_zoom_in((int) m_current_x);
			} else if (delta_y < 0) {
				schedule_zoom_out((int) m_current_x);
			}

		});
		this.setOnKeyPressed(evt -> {
			sendKeyPress(evt);
		});
		this.setOnKeyReleased(evt -> {
			sendKeyRelease(evt);
		});

		m_plot.onRefreshRequired(() -> {
			this.refresh_underlay();
			this.refresh_marker_underlay();
		});
	}

	public void setData(Mda X) {
		m_data = X;
		m_plot.setData(X);
		m_max_timepoint = m_data.size(1) - 1;
		m_num_channels = m_data.size(0);

		set_data_2();
	}

	public void setTimepointsLabels(Mda TL) {
		m_plot.setTimepointsLabels(TL);
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
	}

	void set_data_2() {
		m_current_x = -1;
		CH.trigger("current-x-changed", true);
		refresh_underlay();
		refresh_marker_underlay();
	}

	public void setMarkers(List<SSMarker> markers) {
		m_markers = markers;
		refresh_marker_underlay();
	}

	public void setShowPlots(boolean val) {
		m_plot.setShowPlots(val);

	}

	public void onHoveredXChanged(Runnable X) {
		CH.bind("hovered-x-changed", X);
	}

	public void onCurrentXChanged(Runnable X) {
		CH.bind("current-x-changed", X);
	}

	public void onXRangeChanged(Runnable X) {
		CH.bind("x-range-changed", X);
	}

	public void onCurrentChannelChanged(Runnable X) {
		CH.bind("current-channel-changed", X);
	}

	public void onClicked(Runnable X) {
		CH.bind("clicked", X);
	}

	public void onSelectionRangeChanged(Runnable X) {
		CH.bind("selection-range-changed", X);
	}

	public void sendKeyRelease(KeyEvent evt) {
		if (evt.getCode() == KeyCode.CONTROL) {
			getScene().setCursor(Cursor.DEFAULT);
		}
	}

	public void sendKeyPress(KeyEvent evt) {
		if (evt.getCode() == KeyCode.CONTROL) {
			this.getScene().setCursor(Cursor.MOVE);
		} else if (evt.getCode() == KeyCode.EQUALS) {
			schedule_zoom_in((int) m_current_x);
			//evt.consume();
		} else if (evt.getCode() == KeyCode.MINUS) {
			schedule_zoom_out((int) m_current_x);
			//evt.consume();
		} else if (evt.getCode().equals(KeyCode.LEFT)) {
			if (evt.isControlDown()) {
				do_translate(-1);
			} else {
				move_current_x_coord(-1);
			}
			evt.consume();
		} else if (evt.getCode().equals(KeyCode.RIGHT)) {
			if (evt.isControlDown()) {
				do_translate(+1);
			} else {
				move_current_x_coord(+1);
			}
			evt.consume();
		} else if (evt.getCode().equals(KeyCode.UP)) {
			if (this.currentChannel() + 1 < m_num_channels) {
				this.setCurrentChannel(this.currentChannel() + 1);
			}
		} else if (evt.getCode().equals(KeyCode.DOWN)) {
			if (this.currentChannel() - 1 >= 0) {
				this.setCurrentChannel(this.currentChannel() - 1);
			}
		} else if (evt.getCode().equals(KeyCode.DIGIT0)) {
			zoom_all_the_way_out();
		} else if (evt.getCode().equals(KeyCode.S)) {
			save_image();
		} else if (evt.getCode().equals(KeyCode.M)) {
			m_marker_underlay.setVisible(!m_marker_underlay.isVisible());
		} else if (evt.getCode().equals(KeyCode.C)) {
			m_cursor_visible = !m_cursor_visible;
			refresh_underlay();
		}
	}

	public void setCanZoomAllTheWayOut(boolean val) {
		m_can_zoom_all_the_way_out = val;
	}

	public void setChannelColors(Color[] colors) {
		m_plot.setChannelColors(colors);
	}

	//////////////// PRIVATE ////////////////////
	void save_image() {
		Canvas CC = m_plot.canvas();
		WritableImage wim = new WritableImage((int) CC.getWidth(), (int) CC.getHeight());
		CC.snapshot(null, wim);
		ImageView VV = new ImageView();
		VV.setImage(wim);
		StackPane PP = new StackPane();
		PP.setMinSize(wim.getWidth(), wim.getHeight());
		PP.setMaxSize(wim.getWidth(), wim.getHeight());
		PP.getChildren().add(VV);
		JUtils.popupWidget(PP, "");
	}
	int MIN_PIX_FOR_ZOOM_SELECTION = 1;

	boolean far_enough_away_in_terms_of_pixels(double x1, double x2, int min_pix) {
		Vec2 A = m_plot.coordToPix(new Vec2(x1, 0));
		Vec2 B = m_plot.coordToPix(new Vec2(x2, 0));
		return (Math.abs(A.x - B.x) >= min_pix);
	}

	void on_mouse_released(MouseEvent evt) {
		if (evt.isControlDown()) {
			return;
		}
		Vec2 coord = m_plot.pixToCoord(new Vec2(evt.getX(), evt.getY()));
		if (m_selected_xmin >= 0) {
			m_selected_x_anchor = -1;
		}
	}

	boolean zoom_in_scheduled = false;

	void schedule_zoom_in(int center_x) {
		if (zoom_in_scheduled) {
			return;
		}
		zoom_in_scheduled = true;
		CallbackHandler.scheduleCallback(() -> {
			zoom_in(center_x);
			zoom_in_scheduled = false;
		}, 10);
	}

	void zoom_in(int center_x) {
		if (m_selected_xmin < 0) {
			do_zoom(center_x, 0.8);
		} else {
			do_zoom2(m_selected_xmin, m_selected_xmax);
			this.setSelectionRange(new Vec2(-1, -1));
		}

	}

	boolean zoom_out_scheduled = false;

	void schedule_zoom_out(int center_x) {
		if (zoom_out_scheduled) {
			return;
		}
		zoom_out_scheduled = true;
		CallbackHandler.scheduleCallback(() -> {
			zoom_out(center_x);
			zoom_out_scheduled = false;
		}, 10);
	}

	void zoom_out(int center_x) {
		do_zoom(center_x, 1 / 0.8);
	}

	void do_translate(int dx) {
		int x_left = (int) m_plot.xRange().x;
		int x_right = (int) m_plot.xRange().y;
		x_left += dx;
		x_right += dx;
		if ((x_left >= 0) && (x_right <= m_max_timepoint)) {
			this.setXRange(x_left, x_right);
			this.refresh_underlay();
			this.refresh_marker_underlay();
		}
	}

	void move_current_x_coord(int dx) {
		int new_x = (int) (m_current_x + dx);
		new_x = Math.min(new_x, m_max_timepoint);
		new_x = Math.max(new_x, 0);
		this.setCurrentXCoord(new_x, true);
		CH.trigger(("current-x-changed"), true);
		if (new_x < m_plot.xRange().x) {
			do_translate((int) (new_x - m_plot.xRange().x));
		}
		if (new_x > m_plot.xRange().y) {
			do_translate((int) (new_x - m_plot.xRange().y));
		}
	}

	void zoom_all_the_way_out() {
		if (m_can_zoom_all_the_way_out) {
			int x_left = 0;
			int x_right = m_max_timepoint;
			this.setXRange(x_left, x_right);
			this.refresh_underlay();
			this.refresh_marker_underlay();
		}
	}

	void do_zoom(int center_x, double frac) {
		int xmin = (int) m_plot.xRange().x;
		int xmax = (int) m_plot.xRange().y;
		int diff = xmax - xmin;
		int new_diff = (int) Math.floor(diff * frac);
		if ((new_diff == diff) && (frac > 1)) {
			new_diff += 5;
		}
		if ((new_diff == diff) && (frac < 1)) {
			new_diff -= 5;
		}
		if ((new_diff < 8) && (new_diff < diff)) {
			return;
		}
		int x0 = (int) center_x;
		if (x0 < 0) {
			x0 = (xmax + xmin) / 2;
		}

		//we need to make sure that the hovered point stays in the same place
		//right now, the x0 is in the center. we need to shift it over
		Vec2 pt_left = m_plot.coordToPix(new Vec2(xmin, 0));
		Vec2 pt_right = m_plot.coordToPix(new Vec2(xmax, 0));
		Vec2 pt_hover = m_plot.coordToPix(new Vec2(x0, 0));

		if (pt_right.x <= pt_left.x) {
			return;
		}
		double pct_pt_hover = (pt_hover.x - pt_left.x) / (pt_right.x - pt_left.x);
		//x_left = x0-pct_pt_hover*new_diff
		int x_left = (int) Math.floor(x0 - pct_pt_hover * new_diff);
		int x_right = x_left + new_diff;

		if (x_left < 0) {
			x_left = 0;
			x_right = x_left + new_diff;
		}
		if (x_right > m_max_timepoint) {
			x_right = m_max_timepoint;
			x_left = x_right - new_diff;
			if (x_left < 0) {
				x_left = 0;
			}
		}
		this.setXRange(x_left, x_right);
		this.refresh_underlay();
		this.refresh_marker_underlay();
	}

	void do_zoom2(double xmin, double xmax) {
		this.setXRange(new Vec2(xmin, xmax));
		this.refresh_underlay();
		this.refresh_marker_underlay();
	}

	public void setXRange(int x_left, int x_right) {
		int x1 = Math.max(x_left, 0);
		int x2 = Math.min(m_max_timepoint, x_right);
		if ((x1 == m_plot.xRange().x) && (x2 == m_plot.xRange().y)) {
			return; //important to do this so we don't end up in infinite recursion
		}
		m_plot.setXRange(x1, x2);
		refresh_underlay();
		refresh_marker_underlay();
		CH.trigger("x-range-changed", true);
	}

	public void setXRange(Vec2 range) {
		this.setXRange((int) range.x, (int) range.y);
	}

	public void setSelectionRange(Vec2 range) {
		int x1 = (int) Math.max(range.x, -1);
		int x2 = (int) Math.min(m_max_timepoint, range.y);
		if ((m_selected_xmin == x1) && (m_selected_xmax == x2)) {
			return;
		}
		m_selected_xmin = x1;
		m_selected_xmax = x2;
		m_selected_x_anchor = -1;
		refresh_underlay();
		CH.trigger("selection-range-changed", true);
	}

	public Vec2 xRange() {
		return m_plot.xRange();
	}

	public Vec2 selectionRange() {
		return new Vec2(m_selected_xmin, m_selected_xmax);
	}

	public int hoveredXCoord() {
		return (int) m_hovered_x;
	}

	public int currentXCoord() {
		return (int) m_current_x;
	}

	public int currentChannel() {
		return m_current_channel;
	}

	public void setCurrentXCoord(int x0, boolean do_trigger) {
		if (m_current_x != x0) {
			m_current_x = x0;
			if (do_trigger) {
				CH.trigger("current-x-changed", true);
			}
			this.refresh_underlay();
			move_range_to_current_x_coord_if_necessary();
		}
	}

	public void setCurrentChannel(int ch) {
		setCurrentChannel(ch, true);
	}

	public void setCurrentChannel(int ch, boolean do_trigger) {
		if (ch != m_current_channel) {
			if (ch >= m_num_channels) {
				return; //allow -1
			}
			m_current_channel = ch;
			if (do_trigger) {
				CH.trigger("current-channel-changed", do_trigger);
			}
			this.refresh_underlay();
		}
	}

	////////////////// PRIVATE //////////////////
	void refresh_underlay() {
		GraphicsContext gc = m_underlay.getGraphicsContext2D();

		gc.clearRect(0, 0, getWidth(), getHeight());

		//current channel
		if (m_current_channel >= 0) {
			gc.setFill(Color.LIGHTSALMON);
			gc.setStroke(Color.LIGHTSALMON);
			gc.setLineWidth(2);
			Rect RR = m_plot.channelToRect(m_current_channel);
			//gc.strokeRect(RR.x, RR.y, RR.w, RR.h);
			gc.fillRect(RR.x, RR.y, RR.w, RR.h);
		}

		//hover location
		{
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_hovered_x, 0));
			p0.y = 0;
			p1.y = m_underlay.getHeight();

			gc.beginPath();
			gc.setStroke(Color.rgb(255, 255, 230));
			gc.setLineWidth(2);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.stroke();
		}

		//current location
		{
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_current_x, 0));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_current_x, 0));
			p0.y = 0;
			p1.y = m_underlay.getHeight();

			if (m_cursor_visible) {
				gc.beginPath();
				gc.setStroke(Color.rgb(220, 220, 220));
				gc.setLineWidth(4);
				gc.moveTo(p0.x, p0.y);
				gc.lineTo(p1.x, p1.y);
				gc.stroke();

				gc.beginPath();
				gc.setStroke(Color.rgb(255, 255, 50));
				gc.setLineWidth(2);
				gc.moveTo(p0.x, p0.y);
				gc.lineTo(p1.x, p1.y);
				gc.stroke();
			} else {
			}
		}

		//selected
		if (m_selected_xmin >= 0) {
			double ymin = m_plot.yRange().x;
			double ymax = m_plot.yRange().y;
			Vec2 p0 = m_plot.coordToPix(new Vec2(m_selected_xmin, ymin));
			Vec2 p1 = m_plot.coordToPix(new Vec2(m_selected_xmax, ymax));

			gc.beginPath();
			gc.setStroke(Color.RED);
			if (Math.abs(p0.x - p1.x) < MIN_PIX_FOR_ZOOM_SELECTION) {
				gc.setStroke(Color.GRAY);
			}
			gc.setLineWidth(6);
			gc.moveTo(p0.x, p0.y);
			gc.lineTo(p1.x, p0.y);
			gc.lineTo(p1.x, p1.y);
			gc.lineTo(p0.x, p1.y);
			gc.lineTo(p0.x, p0.y);
			gc.stroke();
		}
	}

	void refresh_marker_underlay() {
		GraphicsContext gc = m_marker_underlay.getGraphicsContext2D();

		gc.clearRect(0, 0, getWidth(), getHeight());

		double xmin = m_plot.xRange().x;
		double xmax = m_plot.xRange().y;

		//if (xmax-xmin>8000) return;
		for (int i = 0; i < m_markers.size(); i++) {
			SSMarker MM = m_markers.get(i);
			if ((xmin <= MM.x) && (MM.x <= xmax)) {

				Vec2 P0 = m_plot.coordToPix(new Vec2(MM.x, m_plot.yRange().x));
				Vec2 P1 = m_plot.coordToPix(new Vec2(MM.x, m_plot.yRange().y));

				gc.setStroke(MM.color);

				gc.beginPath();
				gc.moveTo(P0.x, P0.y);
				gc.lineTo(P1.x, P1.y);
				gc.stroke();
			}
		}
	}

	void move_range_to_current_x_coord_if_necessary() {
		int x0 = (int) m_current_x;
		if (x0 < 0) {
			return;
		}
		Vec2 xrange = m_plot.xRange();
		int diff = (int) (xrange.y - xrange.x);
		if (diff <= 0) {
			return;
		}
		if (x0 <= xrange.x) {
			int x1 = Math.max((int) (x0 - diff * 0.2), 0);
			int x2 = x1 + diff;
			if (x2 < m_data.size(1)) {
				this.setXRange(new Vec2(x1, x2));
			}
		} else if (x0 >= xrange.y) {
			int x2 = Math.min((int) (x0 + diff * 0.2), m_data.size(1));
			int x1 = x2 - diff;
			if (x1 >= 0) {
				System.out.format("setting x range %d,%d\n", x1, x2);
				this.setXRange(new Vec2(x1, x2));
			}
		}
	}
}

class SSMarker {

	double x = 0;
	Color color = Color.BLACK;
}
