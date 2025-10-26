package org.quiltmc.enigma.gui.panel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultiset;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// TODO add marker MouseListener and MouseMotionListener support
public class MarkableScrollPane extends JScrollPane {
	private static final int DEFAULT_MARKER_WIDTH = 10;
	private static final int DEFAULT_MARKER_HEIGHT = 5;

	private static final int DEFAULT_MAX_CONCURRENT_MARKERS = 2;

	private final Multimap<Integer, Marker> markersByPos = Multimaps.newMultimap(new HashMap<>(), TreeMultiset::create);

	private final int markerWidth;
	private final int markerHeight;

	private final int maxConcurrentMarkers;

	@Nullable
	private PaintState paintState;

	/**
	 * Constructs a scroll pane with no view,
	 * {@value DEFAULT_MAX_CONCURRENT_MARKERS} max concurrent markers,
	 * and {@link ScrollBarPolicy#AS_NEEDED AS_NEEDED} scroll bar policies.
	 */
	public MarkableScrollPane() {
		this(null);
	}

	/**
	 * Constructs a scroll pane displaying the passed {@code view},
	 * {@value DEFAULT_MAX_CONCURRENT_MARKERS} max concurrent markers,
	 * and {@link ScrollBarPolicy#AS_NEEDED AS_NEEDED} scroll bar policies.
	 */
	public MarkableScrollPane(Component view) {
		this(view, DEFAULT_MAX_CONCURRENT_MARKERS, ScrollBarPolicy.AS_NEEDED, ScrollBarPolicy.AS_NEEDED);
	}

	/**
	 * @param view                 the component to display in this scroll pane's view port
	 * @param maxConcurrentMarkers the maximum number of markers that will be rendered at the same position;
	 *                             more markers may be added, but only up to this number of markers
	 *                             with the highest priority will be rendered
	 * @param verticalPolicy       the vertical scroll bar policy
	 * @param horizontalPolicy     the horizontal scroll bar policy
	 */
	public MarkableScrollPane(
			@Nullable Component view, int maxConcurrentMarkers,
			ScrollBarPolicy verticalPolicy, ScrollBarPolicy horizontalPolicy
	) {
		super(view, verticalPolicy.vertical, horizontalPolicy.horizontal);

		{
			// DEBUG
			final int crowdedPos = 50;
			this.addMarker(crowdedPos, Color.BLUE, 0);
			this.addMarker(crowdedPos, Color.GREEN, 1);
			// not rendered when maxConcurrentMarkers < 3
			this.addMarker(crowdedPos, Color.PINK, -1);

			this.addMarker(100, Color.CYAN, 0);
		}

		this.markerWidth = ScaleUtil.scale(DEFAULT_MARKER_WIDTH);
		this.markerHeight = ScaleUtil.scale(DEFAULT_MARKER_HEIGHT);

		this.maxConcurrentMarkers = maxConcurrentMarkers;

		this.addComponentListener(new ComponentListener() {
			void refreshMarkers() {
				MarkableScrollPane.this.clearPaintState();
				MarkableScrollPane.this.repaint();
			}

			@Override
			public void componentResized(ComponentEvent e) {
				this.refreshMarkers();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				this.refreshMarkers();
			}

			@Override
			public void componentShown(ComponentEvent e) {
				this.refreshMarkers();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				this.refreshMarkers();
			}
		});
	}

	/**
	 * Adds a marker with passed {@code color} at the given {@code pos}.
	 *
	 * @param pos      the vertical center of the marker within the space of this scroll pane's view
	 * @param color    the color of the marker
	 * @param priority the priority of the marker; if there are multiple markers at the same position, only up to
	 *                 {@link #maxConcurrentMarkers} of the highest priority markers will be rendered
	 * @return         an object which may be used to remove the marker by passing it to {@link #removeMarker(Object)}
	 */
	public Object addMarker(int pos, Color color, int priority) {
		if (pos < 0) {
			throw new IllegalArgumentException("pos must not be negative!");
		}

		final Marker marker = new Marker(color, priority);
		this.markersByPos.put(pos, marker);

		if (this.paintState != null) {
			this.paintState.pendingMarkerPositions.add(pos);
		}

		return marker;
	}

	/**
	 * Removes the passed {@code marker} if it belongs to this scroll pane.
	 *
	 * @param marker an object previously returned by {@link #addMarker(int, Color, int)}
	 */
	public void removeMarker(Object marker) {
		if (marker instanceof Marker removing) {
			final Iterator<Map.Entry<Integer, Marker>> itr = this.markersByPos.entries().iterator();

			while (itr.hasNext()) {
				final Map.Entry<Integer, Marker> entry = itr.next();
				if (entry.getValue() == removing) {
					itr.remove();
					if (this.paintState != null) {
						this.paintState.pendingMarkerPositions.add(entry.getKey());
					}

					break;
				}
			}
		}
	}

	/**
	 * Removes all markers from this scroll pane.
	 */
	public void clearMarkers() {
		this.markersByPos.clear();

		if (this.paintState != null) {
			this.paintState.clearMarkers();
		}
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		if (this.paintState == null) {
			this.paintState = this.createPaintState();
		}

		this.paintState.paint(graphics);
	}

	private void clearPaintState() {
		this.paintState = null;
	}

	private PaintState createPaintState() {
		final Rectangle bounds = this.getBounds();
		final Insets insets = this.getInsets();

		final int verticalScrollBarWidth = this.verticalScrollBar == null || !this.verticalScrollBar.isVisible()
				? 0 : this.verticalScrollBar.getWidth();

		final int viewHeight = this.getViewport().getView().getPreferredSize().height;

		final int areaHeight;
		if (viewHeight < bounds.height) {
			areaHeight = viewHeight;
		} else {
			final int horizontalScrollBarHeight =
					this.horizontalScrollBar == null || !this.horizontalScrollBar.isVisible()
						? 0 : this.horizontalScrollBar.getHeight();

			areaHeight = bounds.height - horizontalScrollBarHeight - insets.top - insets.bottom;
		}

		final int areaX = (int) (bounds.getMaxX() - this.markerWidth - verticalScrollBarWidth - insets.right);
		final int areaY = bounds.y + insets.top;

		return new PaintState(areaX, areaY, areaHeight, viewHeight, this.markersByPos.keySet());
	}

	public enum ScrollBarPolicy {
		AS_NEEDED(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED),
		ALWAYS(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS),
		NEVER(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		private final int horizontal;
		private final int vertical;

		ScrollBarPolicy(int horizontal, int vertical) {
			this.horizontal = horizontal;
			this.vertical = vertical;
		}
	}

	private class PaintState {
		final int areaX;
		final int areaY;
		final int areaHeight;

		final int viewHeight;
		final Set<Integer> pendingMarkerPositions;
		final Map<Integer, MarkersPainter> paintersByPos;

		PaintState(int areaX, int areaY, int areaHeight, int viewHeight, Collection<Integer> pendingMarkerPositions) {
			this.areaX = areaX;
			this.areaY = areaY;
			this.areaHeight = areaHeight;
			this.viewHeight = viewHeight;
			this.pendingMarkerPositions = new HashSet<>(pendingMarkerPositions);
			// order with greatest position first so lesser positions are rendered later and thus on top
			this.paintersByPos = new TreeMap<>(Collections.reverseOrder());
		}

		void paint(Graphics graphics) {
			for (final int pos : this.pendingMarkerPositions) {
				this.refreshPainter(pos, MarkableScrollPane.this.markersByPos.get(pos));
			}

			this.pendingMarkerPositions.clear();

			{
				// DEBUG
				graphics.setColor(new Color(255, 0, 0, 100));
				graphics.fillRect(this.areaX, this.areaY, MarkableScrollPane.this.markerWidth, this.areaHeight);
			}

			for (final MarkersPainter painter : this.paintersByPos.values()) {
				painter.paint(graphics);
			}
		}

		void refreshPainter(int pos, Collection<Marker> markers) {
			if (pos < this.viewHeight && !markers.isEmpty()) {
				final int scaledPos = this.viewHeight > this.areaHeight
						? pos * this.areaHeight / this.viewHeight
						: pos;

				final int markerY = Math.max(scaledPos - MarkableScrollPane.this.markerHeight / 2, 0);
				final int markerHeight = Math.min(MarkableScrollPane.this.markerHeight, this.areaHeight - markerY);

				final List<Marker> posMarkers = markers.stream()
						.limit(MarkableScrollPane.this.maxConcurrentMarkers)
						.toList();

				this.paintersByPos.put(pos, new MarkersPainter(posMarkers, this.areaX, markerY, markerHeight));
			} else {
				this.paintersByPos.remove(pos);
			}
		}

		void clearMarkers() {
			this.paintersByPos.clear();
			this.pendingMarkerPositions.clear();
		}
	}

	private record Marker(Color color, int priority) implements Comparable<Marker> {
		@Override
		public int compareTo(@Nonnull Marker other) {
			return other.priority - this.priority;
		}

		class Span {
			final int x;
			final int width;

			Span(int x, int width) {
				this.x = x;
				this.width = width;
			}

			Marker getMarker() {
				return Marker.this;
			}
		}
	}

	private class MarkersPainter {
		final ImmutableList<Marker.Span> spans;
		final int y;
		final int height;

		MarkersPainter(List<Marker> markers, int x, int y, int height) {
			final int markerCount = markers.size();
			if (markerCount < 1) {
				throw new IllegalArgumentException("no markers!");
			}

			this.y = y;
			this.height = height;

			if (markerCount == 1) {
				this.spans = ImmutableList.of(markers.get(0).new Span(x, MarkableScrollPane.this.markerWidth));
			} else {
				final int spanWidth = MarkableScrollPane.this.markerWidth / markerCount;
				// in case of non-evenly divisible width, give the most to the first marker: it has the highest priority
				final int firstSpanWidth = MarkableScrollPane.this.markerWidth - spanWidth * (markerCount - 1);

				final ImmutableList.Builder<Marker.Span> spansBuilder = ImmutableList.builder();
				spansBuilder.add(markers.get(0).new Span(x, firstSpanWidth));

				for (int i = 1; i < markerCount; i++) {
					spansBuilder.add(markers.get(i).new Span(x + firstSpanWidth + spanWidth * (i - 1), spanWidth));
				}

				this.spans = spansBuilder.build();
			}
		}

		void paint(Graphics graphics) {
			for (final Marker.Span span : this.spans) {
				graphics.setColor(span.getMarker().color);
				graphics.fillRect(span.x, this.y, span.width, this.height);
			}
		}
	}
}
