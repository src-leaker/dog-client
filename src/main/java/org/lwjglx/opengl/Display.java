package org.lwjglx.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjglx.BufferUtils.createByteBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.Canvas;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjglx.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjglx.LWJGLUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.lwjglx.LWJGLException;
import org.lwjglx.Sys;
import org.lwjglx.input.Cursor;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Arrays;

public class Display {

	private static String windowTitle = "Game";

	private static long context;

	private static DisplayImplementation display_impl;

	private static boolean displayCreated = false;
	private static boolean displayFocused = true;
	private static boolean displayVisible = true;
	private static boolean displayDirty = false;
	private static boolean displayResizable = false;

	private static DisplayMode mode, desktopDisplayMode;

	private static int latestEventKey = 0;

	private static int displayX = -1;
	private static int displayY = -1;

	private static boolean displayResized = false;
	private static int displayWidth = 0;
	private static int displayHeight = 0;
	private static int displayFramebufferWidth = 0;
	private static int displayFramebufferHeight = 0;

	private static boolean latestResized = false;
	private static int latestWidth = 0;
	private static int latestHeight = 0;

	private static boolean vsyncEnabled = false;
	private static boolean displayFullscreen = false;
	private static float fps;

	private static boolean window_created;

	/** The Drawable instance that tracks the current Display context */
	private static volatile DrawableLWJGL drawable = null;

	private static Canvas parent;

	private static ByteBuffer[] cached_icons;

	private static int swap_interval;

	public static double xPos = 0, yPos = 0;

	static {
		Sys.initialize(); // init using dummy sys method

		long monitor = glfwGetPrimaryMonitor();
		GLFWVidMode vidmode = glfwGetVideoMode(monitor);

		int monitorWidth = displayWidth = displayFramebufferWidth = vidmode.width();
		int monitorHeight = displayHeight = displayFramebufferHeight = vidmode.height();
		int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
		int monitorRefreshRate = vidmode.refreshRate();

		mode = desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);
		LWJGLUtil.log("Initial mode: " + desktopDisplayMode);
		if("opengles2".equals(System.getenv("POJAV_RENDERER"))) GLContext.getCapabilities();
	}

	public static void setSwapInterval(int value) {
		synchronized ( GlobalLock.lock ) {
			swap_interval = value;
			if ( isCreated() ) {
				drawable.setSwapInterval(swap_interval);

			}
		}
	}

	private static void makeCurrentAndSetSwapInterval() throws LWJGLException {
		makeCurrent();
		try {
			drawable.checkGLError();
		} catch (OpenGLException e) {
			LWJGLUtil.log("OpenGL error during context creation: " + e.getMessage());
		}
		setSwapInterval(swap_interval);
	}

	private static void initContext() {
		drawable.initContext(0, 0, 0);
		update();
	}

	private static void initControls() {
		// Automatically create mouse, keyboard and controller
		if ( true ) {
			if ( !Mouse.isCreated() ) {
				try {
					Mouse.create();
				} catch (LWJGLException e) {
					if ( LWJGLUtil.DEBUG ) {
						e.printStackTrace(System.err);
					} else {
						LWJGLUtil.log("Failed to create Mouse: " + e);
					}
				}
			}
			if ( !Keyboard.isCreated() ) {
				try {
					Keyboard.create();
				} catch (LWJGLException e) {
					if ( LWJGLUtil.DEBUG ) {
						e.printStackTrace(System.err);
					} else {
						LWJGLUtil.log("Failed to create Keyboard: " + e);
					}
				}
			}
		}
	}


	private static void destroyWindow() {
		if ( !window_created ) {
			return;
		}

		// Automatically destroy keyboard & mouse
		if ( Mouse.isCreated() ) {
			Mouse.destroy();
		}
		if ( Keyboard.isCreated() ) {
			Keyboard.destroy();
		}
		display_impl.destroyWindow();
		window_created = false;
	}

	private static void reset() {
		display_impl.resetDisplayMode();
	}

	private static void createWindow() throws LWJGLException {
		if ( window_created ) {
			return;
		}

		DisplayMode mode = Display.getDisplayMode();
		display_impl.createWindow(drawable, mode, null, 0, 0 /* getWindowX(), getWindowY() */);
		window_created = true;

		displayWidth = mode.getWidth();
		displayHeight = mode.getHeight();

		initControls();

		setIcon(new ByteBuffer[] { LWJGLUtil.LWJGLIcon32x32, LWJGLUtil.LWJGLIcon16x16 });
	}

	public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
		// System.out.println("TODO: Implement Display.create(PixelFormat,
		// Drawable)"); // TODO
		create(pixel_format);
	}

	public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
		// System.out.println("TODO: Implement Display.create(PixelFormat,
		// ContextAttribs)"); // TODO
		create(pixel_format);
	}

	public static void create(PixelFormat format) throws LWJGLException {
		glfwWindowHint(GLFW_ACCUM_ALPHA_BITS, format.getAccumulationBitsPerPixel());
		glfwWindowHint(GLFW_ALPHA_BITS, format.getAlphaBits());
		glfwWindowHint(GLFW_AUX_BUFFERS, format.getAuxBuffers());
		glfwWindowHint(GLFW_DEPTH_BITS, format.getDepthBits());
		glfwWindowHint(GLFW_SAMPLES, format.getSamples());
		glfwWindowHint(GLFW_STENCIL_BITS, format.getStencilBits());
		create();
	}

	private static boolean isCreated = false;
	public static void create() throws LWJGLException {
		if (isCreated) return;
		else isCreated = true;

		if (Window.handle != MemoryUtil.NULL)
			glfwDestroyWindow(Window.handle);

		long monitor = glfwGetPrimaryMonitor();
		GLFWVidMode vidmode = glfwGetVideoMode(monitor);

		int monitorWidth = vidmode.width();
		int monitorHeight = vidmode.height();
		int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
		int monitorRefreshRate = vidmode.refreshRate();

		desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

		Window.handle = glfwCreateWindow(mode.getWidth(), mode.getHeight(), windowTitle, isFullscreen() ? glfwGetPrimaryMonitor() : NULL, Window.handle);
		if (Window.handle == NULL)
			throw new LWJGLException("Failed to create Display window");

		Window.scrollCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				Mouse.addScrollEvent(120 * (int) yoffset);
				Mouse.addMoveEvent(xPos, yPos);
			}
		};

		Window.keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				latestEventKey = key;
				boolean isHeldRepeat = (action == GLFW_REPEAT && Keyboard.isRepeatEvent());
				if (action == GLFW_RELEASE || action == GLFW.GLFW_PRESS || isHeldRepeat) {
					Keyboard.addKeyEvent(key, action == GLFW.GLFW_PRESS || isHeldRepeat);
				}
			}
		};

		Window.charCallback = new GLFWCharCallback() {
			@Override
			public void invoke(long window, int codepoint) {
				Keyboard.addCharEvent(latestEventKey, (char)codepoint);
			}
		};

		Window.cursorPosCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				xPos = xpos;
				yPos = ypos;
				Mouse.addMoveEvent(xpos, ypos);
			}
		};

		Window.mouseButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				Mouse.addButtonEvent(button, action == GLFW.GLFW_PRESS);
			}
		};

		Window.windowFocusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, boolean focused) {
				displayFocused = focused;
			}
		};

		Window.windowIconifyCallback = new GLFWWindowIconifyCallback() {
			@Override
			public void invoke(long window, boolean iconified) {
				displayVisible = iconified;
			}
		};

		Window.windowSizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				latestResized = true;
				latestWidth = width;
				latestHeight = height;
			}
		};

		Window.windowPosCallback = new GLFWWindowPosCallback() {
			@Override
			public void invoke(long window, int xpos, int ypos) {
				displayX = xpos;
				displayY = ypos;
			}
		};

		Window.windowRefreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				displayDirty = true;
			}
		};

		Window.framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				displayFramebufferWidth = width;
				displayFramebufferHeight = height;
			}
		};

		Window.setCallbacks();

		displayWidth = mode.getWidth();
		displayHeight = mode.getHeight();

		IntBuffer fbw = BufferUtils.createIntBuffer(1);
		IntBuffer fbh = BufferUtils.createIntBuffer(1);
		glfwGetFramebufferSize(Window.handle, fbw, fbh);
		displayFramebufferWidth = fbw.get(0);
		displayFramebufferHeight = fbh.get(0);



		glfwSetWindowPos(Window.handle, mode.getWidth(), mode.getHeight());

		if (displayX == -1) {
			displayX = (monitorWidth - mode.getWidth()) / 2;
		}

		if (displayY == -1) {
			displayY = (monitorHeight - mode.getHeight()) / 2;
		}

		glfwMakeContextCurrent(Window.handle);
		GL.createCapabilities();

		//glfwMakeContextCurrent(Window.handle);
		final DrawableGL drawable = new DrawableGL() {
			public void destroy() {
				synchronized ( GlobalLock.lock ) {
					if ( !isCreated() )
						return;

					super.destroy();
					destroyWindow();
					// x = y = -1;
					// cached_icons = null;
					reset();
				}
			}
		};
//		drawable.context = new ContextGL();
//		drawable.context.makeCurrent();
//		Display.drawable = drawable;

		//glfwSwapInterval(0);
		glfwShowWindow(Window.handle);
		if(parent != null) parent.setSize(displayWidth, displayHeight);
		Mouse.create();
		Keyboard.create();

		// glfwSetWindowIcon(Window.handle, icons);
		display_impl = new DisplayImplementation() {

			@Override
			public void setNativeCursor(Object handle) throws LWJGLException {
				try {
					Mouse.setNativeCursor((Cursor) handle);
				} catch (ClassCastException e) {
					throw new LWJGLException("Handle is not an instance of cursor");
				}
			}

			@Override
			public void setCursorPosition(int x, int y) {
				Mouse.setCursorPosition(x, y);
			}

			@Override
			public void readMouse(ByteBuffer buffer) {

			}

			@Override
			public void readKeyboard(ByteBuffer buffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void pollMouse(IntBuffer coord_buffer, ByteBuffer buttons) {

			}

			@Override
			public void pollKeyboard(ByteBuffer keyDownBuffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isInsideWindow() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasWheel() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void grabMouse(boolean grab) {
				Mouse.setGrabbed(grab);
			}

			@Override
			public int getNativeCursorCapabilities() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMinCursorSize() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMaxCursorSize() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getButtonCount() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void destroyMouse() {
				// TODO Auto-generated method stub

			}

			@Override
			public void destroyKeyboard() {
				// TODO Auto-generated method stub

			}

			@Override
			public void destroyCursor(Object cursor_handle) {
				// TODO Auto-generated method stub

			}

			@Override
			public void createMouse() throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public void createKeyboard() throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public Object createCursor(int width, int height, int xHotspot, int yHotspot, int numImages,
									   IntBuffer images, IntBuffer delays) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean wasResized() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void update() {
				Display.update();
			}

			@Override
			public void switchDisplayMode(DisplayMode mode) throws LWJGLException {
				Display.setDisplayMode(mode);
			}

			@Override
			public void setTitle(String title) {
				windowTitle = title;
				glfwSetWindowTitle(getHandle(), windowTitle);
			}

			@Override
			public void setResizable(boolean resizable) {
				Display.setResizable(resizable);
			}

			@Override
			public void setPbufferAttrib(PeerInfo handle, int attrib, int value) {
				// TODO Auto-generated method stub

			}

			@Override
			public int setIcon(ByteBuffer[] icons) {
				// TODO Auto-generated method stub
				Display.setIcon(icons);
				return 0;
			}

			@Override
			public void setGammaRamp(FloatBuffer gammaRamp) throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public void reshape(int x, int y, int width, int height) {
				// TODO Auto-generated method stub

			}

			@Override
			public void resetDisplayMode() {
				try {
					Display.setDisplayMode(desktopDisplayMode);
				} catch (LWJGLException e) {
				}
			}

			@Override
			public void releaseTexImageFromPbuffer(PeerInfo handle, int buffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isVisible() {
				// TODO Auto-generated method stub
				return Display.displayVisible;
			}

			@Override
			public boolean isDirty() {
				// TODO Auto-generated method stub
				return Display.displayDirty;
			}

			@Override
			public boolean isCloseRequested() {
				// TODO Auto-generated method stub
				return GLFW.glfwWindowShouldClose(Window.handle);
			}

			@Override
			public boolean isBufferLost(PeerInfo handle) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isActive() {
				// TODO Auto-generated method stub
				return Display.displayFocused;
			}

			@Override
			public DisplayMode init() throws LWJGLException {
				// TODO Auto-generated method stub
				return desktopDisplayMode;
			}

			@Override
			public int getY() {
				// TODO Auto-generated method stub
				return Display.displayY;
			}

			@Override
			public int getX() {
				// TODO Auto-generated method stub
				return Display.displayX;
			}

			@Override
			public int getWidth() {
				// TODO Auto-generated method stub
				return Display.latestWidth;
			}

			@Override
			public String getVersion() {
				// TODO Auto-generated method stub
				return Sys.getVersion();
			}

			@Override
			public float getPixelScaleFactor() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getPbufferCapabilities() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getHeight() {
				// TODO Auto-generated method stub
				return Display.latestHeight;
			}

			@Override
			public int getGammaRampLength() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
				// TODO Auto-generated method stub
				return Display.getAvailableDisplayModes();
			}

			@Override
			public String getAdapter() {
				// TODO Auto-generated method stub
				return Display.getAdapter();
			}

			@Override
			public void destroyWindow() {
				Display.destroy();
			}

			@Override
			public void createWindow(DrawableLWJGL drawable, DisplayMode mode, Canvas parent, int x, int y)
					throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public PeerInfo createPeerInfo(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PeerInfo createPbuffer(int width, int height, PixelFormat pixel_format, ContextAttribs attribs,
										  IntBuffer pixelFormatCaps, IntBuffer pBufferAttribs) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void bindTexImageToPbuffer(PeerInfo handle, int buffer) {
				// TODO Auto-generated method stub

			}
		};


		displayCreated = true;

	}

	public static boolean isCreated() {
		return true;
	}

	public static boolean isActive() {
		return displayFocused;
	}

	public static boolean isVisible() {
		return displayVisible;
	}

	public static long getContext() {
		return context;
	}

	public static void setLocation(int new_x, int new_y) {
		if (Window.handle == 0L) {
			Display.displayX = new_x;
			Display.displayY = new_y;
		} else {
			GLFW.glfwSetWindowPos(Window.handle, new_x, new_y);
		}
	}

	public static void setVSyncEnabled(boolean sync) {
		vsyncEnabled = sync;
		glfwSwapInterval(vsyncEnabled ? 1 : 0);
	}

	public static long getHandle() {
		return Window.handle;
	}

	public static void update() {
		update(true);
	}

	public static void update(boolean processMessages) {
		try {
			swapBuffers();
			displayDirty = false;

		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}

		if (processMessages)
			processMessages();
	}

	public static void processMessages() {
		glfwPollEvents();
		Keyboard.poll();
		Mouse.poll();

		if (latestResized) {
			latestResized = false;
			displayResized = true;
			displayWidth = latestWidth;
			displayHeight = latestHeight;
		} else {
			displayResized = false;
		}
	}

	/** Return the last parent set with setParent(). */
	public static Canvas getParent() {
		return parent;
	}

	/**
	 * Set the parent of the Display. If parent is null, the Display will appear as a top level window.
	 * If parent is not null, the Display is made a child of the parent. A parent's isDisplayable() must be true when
	 * setParent() is called and remain true until setParent() is called again with
	 * null or a different parent. This generally means that the parent component must remain added to it's parent container.<p>
	 * It is not advisable to call this method from an AWT thread, since the context will be made current on the thread
	 * and it is difficult to predict which AWT thread will process any given AWT event.<p>
	 * While the Display is in fullscreen mode, the current parent will be ignored. Additionally, when a non null parent is specified,
	 * the Dispaly will inherit the size of the parent, disregarding the currently set display mode.<p>
	 */
	public static void setParent(Canvas parent) throws LWJGLException {
		if ( Display.parent != parent ) {
			Display.parent = parent;
            /*
            if ( !isCreated() )
                return;
            destroyWindow();
            try {
                if ( isFullscreen() ) {
                    switchDisplayMode();
                } else {
                    display_impl.resetDisplayMode();
                }
                createWindow();
                makeCurrentAndSetSwapInterval();
            } catch (LWJGLException e) {
                drawable.destroy();
                display_impl.resetDisplayMode();
                throw e;
            }
            */
		}
	}

	public static void swapBuffers() throws LWJGLException {
		glfwSwapBuffers(Window.handle);
	}

	public static void destroy() {
		Window.releaseCallbacks();
		glfwDestroyWindow(Window.handle);

		displayCreated = false;
	}

	public static void setDisplayMode(DisplayMode dm) throws LWJGLException {
		mode = dm;
		if(isCreated) GLFW.glfwSetWindowSize(Window.handle, dm.getWidth(), dm.getHeight());
	}

	public static DisplayMode getDisplayMode() {
		return mode;
	}

	public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
		GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());

		DisplayMode[] displayModes = new DisplayMode[modes.capacity()];

		for (int i = 0; i < modes.capacity(); i++) {
			modes.position(i);

			int w = modes.width();
			int h = modes.height();
			int b = modes.redBits() + modes.greenBits() + modes.blueBits();
			int r = modes.refreshRate();

			displayModes[i] = new DisplayMode(w, h, b, r);
		}

		return displayModes;
	}

	public static DisplayMode getDesktopDisplayMode() {
		long mon = GLFW.glfwGetPrimaryMonitor();
		GLFWVidMode mode = GLFW.glfwGetVideoMode(mon);
		return new DisplayMode(mode.width(), mode.height(), mode.redBits() + mode.greenBits() + mode.blueBits(),
				mode.refreshRate());
	}

	public static boolean wasResized() {
		return displayResized;
	}

	public static int getX() {
		return displayX;
	}

	public static int getY() {
		return displayY;
	}

	public static int getWidth() {
		return displayWidth;
	}

	public static int getHeight() {
		return displayHeight;
	}

	public static int getFramebufferWidth() {
		return displayFramebufferWidth;
	}

	public static int getFramebufferHeight() {
		return displayFramebufferHeight;
	}

	public static void setTitle(String title) {
		windowTitle = title;
		if(Window.handle != NULL)
			glfwSetWindowTitle(Window.handle, windowTitle);
	}

	public static String getTitle() { return windowTitle; }

	public static boolean isCloseRequested() {
		return glfwWindowShouldClose(Window.handle) == true;
	}

	public static boolean isDirty() {
		return displayDirty;
	}

	public static void setInitialBackground(float red, float green, float blue) {
		// System.out.println("TODO: Implement Display.setInitialBackground(float, float, float)");

		if (Window.handle != MemoryUtil.NULL) {
			GL11.glClearColor(red, green, blue, 1f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
	}

	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;

		Path path = resource.startsWith("http") ? null : Paths.get(resource);
		if (path != null && Files.isReadable(path)) {
			try (SeekableByteChannel fc = Files.newByteChannel(path)) {
				buffer = createByteBuffer((int)fc.size() + 1);
				while (fc.read(buffer) != -1) {
					;
				}
			}
		} else {
			try (
					InputStream source = resource.startsWith("http")
							? new URL(resource).openStream()
							: Display.class.getClassLoader().getResourceAsStream(resource);
					ReadableByteChannel rbc = Channels.newChannel(source)
			) {
				buffer = createByteBuffer(bufferSize);

				while (true) {
					int bytes = rbc.read(buffer);
					if (bytes == -1) {
						break;
					}
					if (buffer.remaining() == 0) {
						buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2); // 50%
					}
				}
			}
		}

		buffer.flip();
		return memSlice(buffer);
	}

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}

	public static int setIcon(ByteBuffer[] icons) {
		if (!Arrays.equals(cached_icons, icons))
			cached_icons = Arrays.stream(icons)
					.map(Display::cloneByteBuffer)
					.toArray(ByteBuffer[]::new);

		if (isCreated) {
			glfwSetWindowIcon(Window.handle, iconsToGLFWBuffer(cached_icons));
			return 1;
		}

		return 0;
	}

	public static void setResizable(boolean resizable) {
		//displayResizable = resizable;
		if (displayResizable ^ resizable) {
			if (Window.handle != 0) {
				IntBuffer width = BufferUtils.createIntBuffer(1);
				IntBuffer height = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetWindowSize(Window.handle, width, height);
				width.rewind();
				height.rewind();

				GLFW.glfwDefaultWindowHints();
				glfwWindowHint(GLFW_VISIBLE, displayVisible ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

				GLFW.glfwSetWindowSize(Window.handle, width.get(), height.get());
			}
		}
		displayResizable = resizable;
	}

	public static boolean isResizable() {
		return displayResizable;
	}

	public static void setDisplayModeAndFullscreen(DisplayMode dm) throws LWJGLException {
		if(Window.handle != 0) {
			Display.mode = dm;
			GLFW.glfwSetWindowSize(Window.handle, dm.getWidth(), dm.getHeight());
		}
	}

	public static void setFullscreen(boolean fullscreen) throws LWJGLException {
		GLFWVidMode glfwVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if(fullscreen){
			glfwSetWindowMonitor(Window.handle, glfwGetPrimaryMonitor(), displayX, displayY, glfwVidMode.width(), glfwVidMode.height(), glfwVidMode.refreshRate());
		}else{
			glfwSetWindowMonitor(Window.handle, 0, displayX, displayY + 25, mode.getWidth(), mode.getHeight(), glfwVidMode.refreshRate());
		}

		displayFullscreen = fullscreen;
	}

	public static boolean isFullscreen() {
		return displayFullscreen;
	}

	public static void releaseContext() throws LWJGLException {
		glfwMakeContextCurrent(0);
	}

	public static boolean isCurrent() throws LWJGLException {
		return glfwGetCurrentContext() == Window.handle;
	}

	public static void makeCurrent() throws LWJGLException {
		if (!isCurrent()) {
			// -glfwMakeContextCurrent(Window.handle);
		}
	}

	public static void setDisplayConfiguration(float gamma, float brightness, float contrast) throws LWJGLException {
		// ignore call, this is required for a1.1.1
	}

	public static String getAdapter() {
		// TODO
		return "GeNotSupportedAdapter";
	}

	public static String getVersion() {
		// TODO
		return "1.0 NOT SUPPORTED";
	}

	/**
	 * An accurate sync method that will attempt to run at a constant frame
	 * rate. It should be called once every frame.
	 *
	 * @param fps
	 *            - the desired frame rate, in frames per second
	 */
	public static void sync(int fps) {
        /*if (vsyncEnabled)
            Sync.sync(60);
        else*/ Sync.sync(fps);
	}

	public static Drawable getDrawable() {
		return drawable;
	}

	static DisplayImplementation getImplementation() {
		return display_impl;
	}

	public static class Window {
		public static long handle;

		static GLFWKeyCallback keyCallback;
		static GLFWCharCallback charCallback;
		static GLFWCursorEnterCallback cursorEnterCallback;
		static GLFWCursorPosCallback cursorPosCallback;
		static GLFWMouseButtonCallback mouseButtonCallback;
		static GLFWWindowFocusCallback windowFocusCallback;
		static GLFWWindowIconifyCallback windowIconifyCallback;
		static GLFWWindowSizeCallback windowSizeCallback;
		static GLFWWindowPosCallback windowPosCallback;
		static GLFWWindowRefreshCallback windowRefreshCallback;
		static GLFWFramebufferSizeCallback framebufferSizeCallback;
		static GLFWScrollCallback scrollCallback;

		public static void setCallbacks() {
			glfwSetKeyCallback(handle, keyCallback);
			glfwSetCharCallback(handle, charCallback);
			glfwSetCursorEnterCallback(handle, cursorEnterCallback);
			glfwSetCursorPosCallback(handle, cursorPosCallback);
			glfwSetMouseButtonCallback(handle, mouseButtonCallback);
			glfwSetWindowFocusCallback(handle, windowFocusCallback);
			glfwSetWindowIconifyCallback(handle, windowIconifyCallback);
			glfwSetWindowSizeCallback(handle, windowSizeCallback);
			glfwSetWindowPosCallback(handle, windowPosCallback);
			glfwSetWindowRefreshCallback(handle, windowRefreshCallback);
			glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback);
			glfwSetScrollCallback(handle, scrollCallback);
		}

		public static void releaseCallbacks() {
			Callbacks.glfwFreeCallbacks(handle);
			keyCallback = null;
			charCallback = null;
			cursorEnterCallback = null;
			cursorPosCallback = null;
			mouseButtonCallback = null;
			windowFocusCallback = null;
			windowIconifyCallback = null;
			windowSizeCallback = null;
			windowPosCallback = null;
			windowRefreshCallback = null;
			framebufferSizeCallback = null;
			scrollCallback = null;
			System.gc();
		}
	}

	private static ByteBuffer cloneByteBuffer(ByteBuffer original) {
		ByteBuffer clone = org.lwjglx.BufferUtils.createByteBuffer(original.capacity());
		int oldPosition = original.position();
		clone.put(original);
		((Buffer) original).position(oldPosition);
		((Buffer) clone).flip();

		return clone;
	}

	private static GLFWImage.Buffer iconsToGLFWBuffer(ByteBuffer[] icons) {
		GLFWImage.Buffer buffer = GLFWImage.create(icons.length);
		for (ByteBuffer icon : icons) {
			int size = icon.limit() / 4;
			int dimension = (int) Math.sqrt(size);
			GLFWImage image = GLFWImage.malloc();
			image.set(dimension, dimension, icon);
			buffer.put(image);
		}
		buffer.flip();
		return buffer;
	}
}