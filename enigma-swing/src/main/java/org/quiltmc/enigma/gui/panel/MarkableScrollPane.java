package org.quiltmc.enigma.gui.panel;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * A scroll pane that renders markers in its view along the right edge, to the left of the vertical scroll bar.<br>
 * Markers support custom {@linkplain Color colors} and {@linkplain MarkerListener listeners}.
 *
 * <p> Markers are associated with a vertical position within the vertical space of this scroll pane's view;
 * markers with a position greater than the height of the current view are not rendered.
 * Multiple markers may be rendered at the same position. Markers with the highest priority (specified when
 * {@linkplain #addMarker(int, Color, int, MarkerListener) added}) will be rendered left-most.
 * No more than {@link #maxConcurrentMarkers} will be rendered at the same position. If there are excess markers, those
 * with lowest priority will be skipped. There's no guarantee which marker will be rendered when priorities are tied.
 * When multiple markers are rendered at the same location, each will be narrower so the total width remains constant
 * regardless of the number of markers at a position.
 *
 * @see #addMarker(int, Color, int, MarkerListener)
 * @see #removeMarker(Object)
 * @see MarkerListener
 */
public class MarkableScrollPane extends JScrollPane {
	private static void requireNonNegative(int value, String name) {
		Preconditions.checkArgument(value >= 0, "%s (%s) must not be negative!".formatted(name, value));
	}

	private static final int DEFAULT_MARKER_WIDTH = 10;
	private static final int DEFAULT_MARKER_HEIGHT = 5;

	private final Multimap<Integer, Marker> markersByPos = Multimaps.newMultimap(new HashMap<>(), TreeMultiset::create);

	private final int markerWidth;
	private final int markerHeight;

	private int maxConcurrentMarkers;

	@Nullable
	private PaintState paintState;
	private MouseAdapter viewMouseAdapter;

	/**
	 * Constructs a scroll pane displaying the passed {@code view} and {@code maxConcurrentMarkers},
	 * and {@link ScrollBarPolicy#AS_NEEDED AS_NEEDED} scroll bar policies.
	 *
	 * @see #MarkableScrollPane(Component, int, ScrollBarPolicy, ScrollBarPolicy)
	 */
	public MarkableScrollPane(@Nullable Component view, int maxConcurrentMarkers) {
		this(view, maxConcurrentMarkers, ScrollBarPolicy.AS_NEEDED, ScrollBarPolicy.AS_NEEDED);
	}

	/**
	 * @param view                 the component to display in this scroll pane's view port
	 * @param maxConcurrentMarkers see {@link #setMaxConcurrentMarkers(int)}
	 * @param verticalPolicy       the vertical scroll bar policy
	 * @param horizontalPolicy     the horizontal scroll bar policy
	 *
	 * @throws IllegalArgumentException if {@code maxConcurrentMarkers} is negative
	 *
	 * @see #addMarker(int, Color, int, MarkerListener)
	 */
	public MarkableScrollPane(
			@Nullable Component view, int maxConcurrentMarkers,
			ScrollBarPolicy verticalPolicy, ScrollBarPolicy horizontalPolicy
	) {
		super(view, verticalPolicy.vertical, horizontalPolicy.horizontal);

		this.setMaxConcurrentMarkers(maxConcurrentMarkers);

		this.markerWidth = ScaleUtil.scale(DEFAULT_MARKER_WIDTH);
		this.markerHeight = ScaleUtil.scale(DEFAULT_MARKER_HEIGHT);

		this.addComponentListener(new ComponentListener() {
			void refreshMarkers() {
				MarkableScrollPane.this.paintState = MarkableScrollPane.this.createPaintState();

				// I tried repainting only the old and new paintState areas here, but it sometimes left artifacts of
				// previously painted markers when quickly resizing a right-side docker.
				// Doing a full repaint avoids that artifacting.
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
			public void componentShown(ComponentEvent e) { }

			@Override
			public void componentHidden(ComponentEvent e) {
				MarkableScrollPane.this.paintState = null;
			}
		});
	}

	/**
	 * Adds a marker with passed {@code color} at the given {@code pos}.
	 *
	 * @param pos            the vertical center of the marker within the space of this scroll pane's view;
	 *                       must not be negative; if greater than the height of the current view,
	 *                       the marker will not be rendered
	 * @param color          the color of the marker
	 * @param priority       the priority of the marker; if there are multiple markers at the same position, only up to
	 *                       {@link #maxConcurrentMarkers} of the highest priority markers will be rendered
	 * @param listener  	 a listener for events within the marker; may be {@code null}
	 *
	 * @return an object which may be used to remove the marker by passing it to {@link #removeMarker(Object)}
	 *
	 * @throws IllegalArgumentException if {@code pos} is negative
	 *
	 * @see #removeMarker(Object)
	 * @see MarkerListener
	 */
	public Object addMarker(int pos, Color color, int priority, @Nullable MarkerListener listener) {
		requireNonNegative(pos, "pos");

		final Marker marker = new Marker(color, priority, pos, Optional.ofNullable(listener));

		this.markersByPos.put(pos, marker);

		if (this.paintState == null) {
			this.paintState = this.createPaintState();
		}

		this.paintState.pendingMarkerPositions.add(pos);
		this.repaint(this.paintState.createArea());

		return marker;
	}

	/**
	 * Removes the passed {@code marker} if it belongs to this scroll pane.
	 *
	 * @param marker an object previously returned by {@link #addMarker(int, Color, int, MarkerListener)}
	 *
	 * @see #addMarker(int, Color, int, MarkerListener)
	 * @see #clearMarkers()
	 */
	public void removeMarker(Object marker) {
		if (marker instanceof Marker removing) {
			final boolean removed = this.markersByPos.remove(removing.pos, removing);
			if (removed) {
				if (this.paintState == null) {
					this.paintState = this.createPaintState();
				}

				this.paintState.pendingMarkerPositions.add(removing.pos);
				this.repaint(this.paintState.createArea());
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

	/**
	 * @param maxConcurrentMarkers a (non-negative) number limiting how many markers will be rendered at the same position;
	 *            more markers may be added, but only up to this number of markers with the highest priority will be
	 *            rendered
	 *
	 * @throws IllegalArgumentException if {@code maxConcurrentMarkers} is negative
	 */
	public void setMaxConcurrentMarkers(int maxConcurrentMarkers) {
		requireNonNegative(maxConcurrentMarkers, "maxConcurrentMarkers");

		if (maxConcurrentMarkers != this.maxConcurrentMarkers) {
			this.maxConcurrentMarkers = maxConcurrentMarkers;

			this.paintState = this.createPaintState();
			this.repaint(this.paintState.createArea());
		}
	}

	@Override
	public void setViewportView(Component view) {
		final Component oldView = this.getViewport().getView();
		if (oldView != null) {
			oldView.removeMouseListener(this.viewMouseAdapter);
			oldView.removeMouseMotionListener(this.viewMouseAdapter);
		}

		super.setViewportView(view);

		this.viewMouseAdapter = new MouseAdapter() {
			@Nullable
			ListenerPos lastListenerPos;

			Optional<ListenerPos> findListenerPos(MouseEvent e) {
				if (MarkableScrollPane.this.paintState == null) {
					return Optional.empty();
				} else {
					final Point relativePos = GuiUtil
							.getRelativePos(MarkableScrollPane.this, e.getXOnScreen(), e.getYOnScreen());
					return MarkableScrollPane.this.paintState.findListenerPos(relativePos.x, relativePos.y);
				}
			}

			void tryMarkerListeners(MouseEvent e, ListenerMethod method) {
				this.findListenerPos(e).ifPresent(listenerPos -> listenerPos.invoke(method));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				this.tryMarkerListeners(e, MarkerListener::mouseClicked);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				this.mouseExitedImpl();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				this.tryMarkerListeners(e, MarkerListener::mouseMoved);

				this.findListenerPos(e).ifPresentOrElse(
						listenerPos -> {
							if (!listenerPos.equals(this.lastListenerPos)) {
								if (this.lastListenerPos == null) {
									listenerPos.invoke(MarkerListener::mouseEntered);
								} else {
									listenerPos.invoke(MarkerListener::mouseTransferred);
								}

								this.lastListenerPos = listenerPos;
							}
						},
						this::mouseExitedImpl
				);
			}

			private void mouseExitedImpl() {
				if (this.lastListenerPos != null) {
					this.lastListenerPos.invoke(MarkerListener::mouseExited);
					this.lastListenerPos = null;
				}
			}
		};

		// add the listener to the view because this doesn't receive clicks within the view
		view.addMouseListener(this.viewMouseAdapter);
		view.addMouseMotionListener(this.viewMouseAdapter);
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		if (this.paintState == null) {
			this.paintState = this.createPaintState();
		}

		this.paintState.paint(graphics);
	}

	private PaintState createPaintState() {
		final Rectangle bounds = this.getBounds();
		final Insets insets = this.getInsets();

		final int verticalScrollBarWidth = this.verticalScrollBar == null || !this.verticalScrollBar.isVisible()
				? 0 : this.verticalScrollBar.getWidth();

		final Component view = this.getViewport().getView();
		final int viewHeight = view.getPreferredSize().height;

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

	private MarkersPainter markersPainterOf(List<Marker> markers, int scaledPos, int x, int y, int height) {
		final int markerCount = markers.size();
		Preconditions.checkArgument(markerCount > 0, "no markers!");

		if (markerCount == 1) {
			final ImmutableList<Marker.Span> spans = ImmutableList
					.of(markers.get(0).new Span(x, MarkableScrollPane.this.markerWidth));
			return new MarkersPainter(spans, scaledPos, y, height);
		} else {
			final int spanWidth = MarkableScrollPane.this.markerWidth / markerCount;
			// in case of non-evenly divisible width, give the most to the first marker: it has the highest priority
			final int firstSpanWidth = MarkableScrollPane.this.markerWidth - spanWidth * (markerCount - 1);

			final ImmutableList.Builder<Marker.Span> spansBuilder = ImmutableList.builder();
			spansBuilder.add(markers.get(0).new Span(x, firstSpanWidth));

			for (int i = 1; i < markerCount; i++) {
				spansBuilder.add(markers.get(i).new Span(x + firstSpanWidth + spanWidth * (i - 1), spanWidth));
			}

			return new MarkersPainter(spansBuilder.build(), scaledPos, y, height);
		}
	}

	public enum ScrollBarPolicy {
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_AS_NEEDED
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_AS_NEEDED
		 */
		AS_NEEDED(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED),
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_ALWAYS
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_ALWAYS
		 */
		ALWAYS(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS),
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_NEVER
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_NEVER
		 */
		NEVER(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		private final int horizontal;
		private final int vertical;

		ScrollBarPolicy(int horizontal, int vertical) {
			this.horizontal = horizontal;
			this.vertical = vertical;
		}
	}

	private class PaintState {
		final NavigableMap<Integer, MarkersPainter> paintersByPos = new TreeMap<>();

		final int areaX;
		final int areaY;
		final int areaHeight;

		final int viewHeight;
		final Set<Integer> pendingMarkerPositions;

		PaintState(int areaX, int areaY, int areaHeight, int viewHeight, Collection<Integer> pendingMarkerPositions) {
			this.areaX = areaX;
			this.areaY = areaY;
			this.areaHeight = areaHeight;
			this.viewHeight = viewHeight;
			this.pendingMarkerPositions = new HashSet<>(pendingMarkerPositions);
		}

		void paint(Graphics graphics) {
			this.update();

			for (final MarkersPainter painter : this.paintersByPos.values()) {
				painter.paint(graphics);
			}
		}

		void update() {
			if (this.pendingMarkerPositions.isEmpty()) {
				return;
			}

			final Map<Integer, MarkersPainterBuilder> builderByScaledPos = new TreeMap<>();

			for (final int pos : this.pendingMarkerPositions) {
				final Collection<Marker> markers = MarkableScrollPane.this.markersByPos.get(pos);
				if (pos < this.viewHeight && !markers.isEmpty() && MarkableScrollPane.this.maxConcurrentMarkers > 0) {
					final int scaledPos = this.scalePos(pos) + this.areaY;

					this.paintersByPos.remove(pos);

					final MarkersPainterBuilder builder = builderByScaledPos.computeIfAbsent(scaledPos, builderPos -> {
						final int top = Math.max(builderPos - MarkableScrollPane.this.markerHeight / 2, this.areaY);
						final int bottom = Math.min(top + MarkableScrollPane.this.markerHeight, this.areaY + this.areaHeight);

						return new MarkersPainterBuilder(pos, scaledPos, top, bottom);
					});

					builder.addMarkers(markers);

					Integer abovePos = this.paintersByPos.lowerKey(pos);
					while (abovePos != null) {
						final int aboveScaled = this.scalePos(abovePos);
						if (aboveScaled == scaledPos) {
							this.paintersByPos.remove(abovePos);
							builder.addMarkers(MarkableScrollPane.this.markersByPos.get(abovePos));

							abovePos = this.paintersByPos.lowerKey(abovePos);
						} else {
							break;
						}
					}

					Integer belowPos = this.paintersByPos.higherKey(pos);
					while (belowPos != null) {
						final int belowScaled = this.scalePos(belowPos);
						if (belowScaled == scaledPos) {
							this.paintersByPos.remove(belowPos);
							builder.addMarkers(MarkableScrollPane.this.markersByPos.get(belowPos));

							belowPos = this.paintersByPos.higherKey(belowPos);
						} else {
							break;
						}
					}
				} else {
					this.paintersByPos.remove(pos);
				}
			}

			if (!builderByScaledPos.isEmpty()) {
				final Iterator<MarkersPainterBuilder> buildersItr = builderByScaledPos.values().iterator();
				MarkersPainterBuilder currentBuilder = buildersItr.next();
				while (true) {
					final Map.Entry<Integer, MarkersPainter> above = this.paintersByPos.lowerEntry(currentBuilder.pos);
					if (above != null) {
						final MarkersPainter aboveReplacement = currentBuilder.eliminateOverlap(above.getValue());
						if (aboveReplacement != null) {
							above.setValue(aboveReplacement);
						}
					}

					final Map.Entry<Integer, MarkersPainter> below = this.paintersByPos.higherEntry(currentBuilder.pos);
					if (below != null) {
						final MarkersPainter belowReplacement = currentBuilder.eliminateOverlap(below.getValue());
						if (belowReplacement != null) {
							below.setValue(belowReplacement);
						}
					}

					if (buildersItr.hasNext()) {
						final MarkersPainterBuilder nextBuilder = buildersItr.next();
						currentBuilder.eliminateOverlap(nextBuilder);
						currentBuilder = nextBuilder;
					} else {
						break;
					}
				}

				for (final MarkersPainterBuilder builder : builderByScaledPos.values()) {
					this.paintersByPos.put(builder.pos, builder.build(this.areaX));
				}
			}

			this.pendingMarkerPositions.clear();
		}

		private int scalePos(int pos) {
			return this.viewHeight > this.areaHeight
				? pos * this.areaHeight / this.viewHeight
				: pos;
		}

		void clearMarkers() {
			this.paintersByPos.clear();
			this.pendingMarkerPositions.clear();
		}

		Optional<ListenerPos> findListenerPos(int x, int y) {
			if (this.areaContains(x, y)) {
				return this.paintersByPos.values().stream()
					.filter(painter -> painter.y <= y && y <= painter.y + painter.height)
					.flatMap(painter -> painter
						.spans.stream()
						.filter(span -> span.getMarker().listener.isPresent())
						.filter(span -> span.x <= x && x <= span.x + span.width)
						.findFirst()
						.map(span -> {
							final Point absolutePos = GuiUtil
									.getAbsolutePos(MarkableScrollPane.this, this.areaX, painter.scaledPos);

							return new ListenerPos(span.getMarker().listener.orElseThrow(), absolutePos.x, absolutePos.y);
						})
						.stream()
					)
					.findFirst();
			} else {
				return Optional.empty();
			}
		}

		boolean areaContains(int x, int y) {
			return this.areaX <= x && x <= this.areaX + MarkableScrollPane.this.markerWidth
				&& this.areaY <= y && y <= this.areaY + this.areaHeight;
		}

		Rectangle createArea() {
			return new Rectangle(this.areaX, this.areaY, MarkableScrollPane.this.markerWidth, this.areaHeight);
		}
	}

	private record Marker(Color color, int priority, int pos, Optional<MarkerListener> listener)
			implements Comparable<Marker> {
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

	private record MarkersPainter(ImmutableList<Marker.Span> spans, int scaledPos, int y, int height) {
		void paint(Graphics graphics) {
			for (final Marker.Span span : this.spans) {
				graphics.setColor(span.getMarker().color);
				graphics.fillRect(span.x, this.y, span.width, this.height);
			}
		}

		MarkersPainter withTopMoved(int amount) {
			return new MarkersPainter(this.spans, this.scaledPos, this.y + amount, this.height - amount);
		}

		MarkersPainter withBottomMoved(int amount) {
			return new MarkersPainter(this.spans, this.scaledPos, this.y, this.height + amount);
		}
	}

	private class MarkersPainterBuilder {
		final int pos;
		final int scaledPos;

		final Multiset<Marker> markers = TreeMultiset.create();

		int top;
		int bottom;

		MarkersPainterBuilder(int pos, int scaledPos, int top, int bottom) {
			this.pos = pos;
			this.scaledPos = scaledPos;

			this.top = top;
			this.bottom = bottom;
		}

		void addMarkers(Collection<Marker> markers) {
			this.markers.addAll(markers);
		}

		void eliminateOverlap(MarkersPainterBuilder lower) {
			final int spaceBelow = lower.top - this.bottom;
			if (spaceBelow < 0) {
				final int thisAdjustment = spaceBelow / 2;
				final int otherAdjustment = spaceBelow - thisAdjustment;

				this.bottom += thisAdjustment;
				lower.top -= otherAdjustment;
			}
		}

		@Nullable
		MarkersPainter eliminateOverlap(MarkersPainter painter) {
			final int spaceAbove = painter.y + painter.height - this.top;
			if (spaceAbove < 0) {
				final int painterAdjustment = spaceAbove / 2;
				final int thisAdjustment = spaceAbove - painterAdjustment;

				this.top -= thisAdjustment;
				return painter.withBottomMoved(painterAdjustment);
			} else {
				final int spaceBelow = painter.y - this.bottom;
				if (spaceBelow < 0) {
					final int thisAdjustment = spaceBelow / 2;
					final int painterAdjustment = spaceBelow - thisAdjustment;

					this.bottom += thisAdjustment;
					return painter.withTopMoved(painterAdjustment);
				}
			}

			return null;
		}

		MarkersPainter build(int x) {
			final List<Marker> markers = this.markers.stream()
					.limit(MarkableScrollPane.this.maxConcurrentMarkers)
					.toList();

			return MarkableScrollPane.this.markersPainterOf(markers, this.scaledPos, x, this.top, this.bottom - this.top);
		}
	}

	/**
	 * A listener for marker events.
	 *
	 * <p> Listener methods receive the absolute position of the marker's left edge on the screen.
	 *
	 * @see #addMarker(int, Color, int, MarkerListener)
	 */
	public interface MarkerListener {
		/**
		 * Called when the mouse clicks the marker.
		 */
		void mouseClicked(int x, int y);

		/**
		 * Called when the mouse enters the marker.
		 */
		void mouseEntered(int x, int y);

		/**
		 * Called when the mouse exits the marker.
		 *
		 * <p> <em>Not</em> called when the mouse moves to an adjacent marker;
		 * see {@link #mouseTransferred}.
		 */
		void mouseExited(int x, int y);

		/**
		 * Called when the mouse moves from an adjacent marker to the marker.
		 */
		void mouseTransferred(int x, int y);

		/**
		 * Called when the mouse within the marker.
		 */
		void mouseMoved(int x, int y);
	}

	private record ListenerPos(MarkerListener listener, int x, int y) {
		void invoke(ListenerMethod method) {
			method.listen(this.listener, this.x, this.y);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ListenerPos other && other.listener == this.listener;
		}
	}

	@FunctionalInterface
	private interface ListenerMethod {
		void listen(MarkerListener listener, int x, int pos);
	}
}
