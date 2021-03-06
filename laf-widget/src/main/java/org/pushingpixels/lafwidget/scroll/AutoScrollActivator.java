/*
 * Copyright (c) 2005-2010 Laf-Widget Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of Laf-Widget Kirill Grouchnikov nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.pushingpixels.lafwidget.scroll;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Christopher Deckers (chrriis@nextencia.net) http://www.nextencia.net
 * 
 * @author Christopher Deckers
 */
public class AutoScrollActivator {

	protected JScrollPane scrollPane;

	public AutoScrollActivator(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
		configureScrollPane();
	}

	protected static class AutoScrollProperties {
		public Point startLocation;
		public Point currentLocation;
		public Timer timer;
		public AWTEventListener toolkitListener;
		public boolean isDragMode;
		public JPopupMenu iconPopupMenu;
	}

	protected AutoScrollProperties autoScrollProperties;

	protected void deactivateAutoScroll() {
		if (autoScrollProperties == null)
			return;
		autoScrollProperties.timer.stop();
		Toolkit.getDefaultToolkit().removeAWTEventListener(
				autoScrollProperties.toolkitListener);
		autoScrollProperties.iconPopupMenu.setVisible(false);
		autoScrollProperties = null;
	}

	protected void activateAutoScroll(MouseEvent e) {
		autoScrollProperties = new AutoScrollProperties();
		autoScrollProperties.isDragMode = false;
		JViewport viewport = scrollPane.getViewport();
        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi == null) return; // mouse not on any device
        
        autoScrollProperties.currentLocation = pi.getLocation();
		SwingUtilities.convertPointFromScreen(
				autoScrollProperties.currentLocation, viewport);
		autoScrollProperties.startLocation = autoScrollProperties.currentLocation;
		// We use a popup menu so that it can be heavyweight or lightweight
		// depending on the context.
		// By default it is probably lightweight and thus uses alpha
		// transparency
		final JPopupMenu iconPopupMenu = new JPopupMenu() {
			@Override
			public void setBorder(Border border) {
				// Overriden to avoid having a border set by the L&F
			}
		};
		iconPopupMenu.setFocusable(false);
		iconPopupMenu.setOpaque(false);
		JLabel iconLabel = new JLabel(getAutoScrollIcon());
		iconLabel.addMouseWheelListener(new MouseWheelListener() {
			@Override
            public void mouseWheelMoved(MouseWheelEvent e) {
				deactivateAutoScroll();
			}
		});
		iconPopupMenu.add(iconLabel);
		iconPopupMenu.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				iconPopupMenu.setVisible(false);
			}
		});
		autoScrollProperties.iconPopupMenu = iconPopupMenu;
		Dimension iconPopupMenuSize = iconPopupMenu.getPreferredSize();
		iconPopupMenu.show(viewport, autoScrollProperties.startLocation.x
				- iconPopupMenuSize.width / 2,
				autoScrollProperties.startLocation.y - iconPopupMenuSize.height
						/ 2);
		// Assumption: the popup menu has a parent that is itself added to the
		// glass pane or to a window.
		// Some L&F will create borders to that parent, and we don't want that.
		Container parent = iconPopupMenu.getParent();
		if (parent instanceof JComponent) {
			((JComponent) parent).setBorder(null);
		}
		ActionListener actionListener = new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				JViewport viewport = scrollPane.getViewport();
				Component view = viewport.getView();
				if (view == null) {
					return;
				}
				Point viewPosition = viewport.getViewPosition();
				int offsetX = autoScrollProperties.currentLocation.x
						- autoScrollProperties.startLocation.x;
				int offsetY = autoScrollProperties.currentLocation.y
						- autoScrollProperties.startLocation.y;
				offsetX = offsetX > 0 ? Math.max(0, offsetX - 4) : Math.min(0,
						offsetX + 4);
				offsetY = offsetY > 0 ? Math.max(0, offsetY - 4) : Math.min(0,
						offsetY + 4);
				viewPosition = new Point(viewPosition.x + offsetX,
						viewPosition.y + offsetY);
				Dimension extentSize = viewport.getExtentSize();
				Dimension viewSize = view.getSize();
				if (viewSize.width - viewPosition.x < extentSize.width) {
					viewPosition.x = viewSize.width - extentSize.width;
				}
				if (viewSize.height - viewPosition.y < extentSize.height) {
					viewPosition.y = viewSize.height - extentSize.height;
				}
				if (viewPosition.x < 0) {
					viewPosition.x = 0;
				}
				if (viewPosition.y < 0) {
					viewPosition.y = 0;
				}
				viewport.setViewPosition(viewPosition);
			}
		};
		autoScrollProperties.timer = new Timer(50, actionListener);
		autoScrollProperties.timer.start();
		autoScrollProperties.toolkitListener = new AWTEventListener() {
			@Override
            public void eventDispatched(AWTEvent e) {
				int eventID = e.getID();
				switch (eventID) {
				case MouseEvent.MOUSE_MOVED:
				case MouseEvent.MOUSE_DRAGGED:
					JViewport viewport = scrollPane.getViewport();
                    PointerInfo pi = MouseInfo.getPointerInfo();
                    if (pi == null) break; // pointer not on any device
                    
                    autoScrollProperties.currentLocation = pi.getLocation();
					SwingUtilities.convertPointFromScreen(
							autoScrollProperties.currentLocation, viewport);
					if (!autoScrollProperties.isDragMode
							&& eventID == MouseEvent.MOUSE_DRAGGED) {
						Dimension size = new Dimension(
								Math.abs(autoScrollProperties.currentLocation.x
										- autoScrollProperties.startLocation.x),
								Math.abs(autoScrollProperties.currentLocation.y
										- autoScrollProperties.startLocation.y));
						autoScrollProperties.isDragMode = size.width > HV_IMAGE_ICON
								.getIconWidth() / 2
								|| size.height > HV_IMAGE_ICON.getIconHeight() / 2;
					}
					break;
				case MouseEvent.MOUSE_PRESSED:
				case MouseEvent.MOUSE_WHEEL:
					deactivateAutoScroll();
					break;
				case MouseEvent.MOUSE_RELEASED:
					if (autoScrollProperties.isDragMode
							&& ((MouseEvent) e).getButton() == 2) {
						deactivateAutoScroll();
					}
					break;
				case WindowEvent.WINDOW_LOST_FOCUS:
					deactivateAutoScroll();
					break;
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(
				autoScrollProperties.toolkitListener,
				AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
						| AWTEvent.MOUSE_WHEEL_EVENT_MASK
						| AWTEvent.WINDOW_FOCUS_EVENT_MASK);
	}

	protected static class AutoScrollMouseListener extends MouseAdapter {
		protected AutoScrollActivator autoScrollActivator;

		public AutoScrollMouseListener(AutoScrollActivator autoScrollActivator) {
			this.autoScrollActivator = autoScrollActivator;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() != 2) {
				return;
			}
			autoScrollActivator.activateAutoScroll(e);
		}
	}

	protected void configureScrollPane() {
		for (MouseListener mouseListener : scrollPane.getMouseListeners()) {
			if (mouseListener instanceof AutoScrollMouseListener) {
				return;
			}
		}
		scrollPane.addMouseListener(new AutoScrollMouseListener(this));
	}

	protected static final ImageIcon H_IMAGE_ICON = new ImageIcon(
			AutoScrollActivator.class.getResource("resource/autoscroll_h.png"));
	protected static final ImageIcon V_IMAGE_ICON = new ImageIcon(
			AutoScrollActivator.class.getResource("resource/autoscroll_v.png"));
	protected static final ImageIcon HV_IMAGE_ICON = new ImageIcon(
			AutoScrollActivator.class
					.getResource("resource/autoscroll_all.png"));

	protected ImageIcon getAutoScrollIcon() {
		ImageIcon icon;
		if (scrollPane.getHorizontalScrollBar().isVisible()) {
			if (scrollPane.getVerticalScrollBar().isVisible()) {
				icon = HV_IMAGE_ICON;
			} else {
				icon = H_IMAGE_ICON;
			}
		} else {
			if (scrollPane.getVerticalScrollBar().isVisible()) {
				icon = V_IMAGE_ICON;
			} else {
				icon = HV_IMAGE_ICON;
			}
		}
		return icon;
	}

	public static void setAutoScrollEnabled(final JScrollPane scrollPane,
			boolean isEnabled) {
		if (isEnabled) {
			new AutoScrollActivator(scrollPane);
		} else {
			for (MouseListener mouseListener : scrollPane.getMouseListeners()) {
				if (mouseListener instanceof AutoScrollMouseListener) {
					scrollPane.removeMouseListener(mouseListener);
					return;
				}
			}
		}
	}

}
