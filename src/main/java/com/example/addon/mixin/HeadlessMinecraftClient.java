package com.example.addon.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(MinecraftClient.class)
public class HeadlessMinecraftClient {

//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void onInit(CallbackInfo ci) {
//        startCommandThread();
//        System.out.println("#########################################");
//    }

    BufferedReader reader;
    PrintWriter writer;

    @Shadow private Thread thread;


    @Shadow
    @Final
    public InGameHud inGameHud;
    @Unique final MinecraftClient self = (MinecraftClient)(Object)this;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onGameLoaded(RunArgs args, CallbackInfo ci) {
//        AddonTemplate.LOG.info("Hello from ExampleMixin!");
        System.out.println("#########################################");
        System.out.println("#########################################");
        System.out.println("(no-more-render) This client is running a mod that disables Minecraft from opening a window!");
        System.out.println("#########################################");
        System.out.println("#########################################");

        startCommandThread();
        self.gameRenderer.onResized(10, 10);
    }


    private void startCommandThread() {
        Thread commandThread = new Thread(() -> {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            int i = 0;
            try {
                ServerSocket serverSocket;
                while(true){
                    try {
                        serverSocket = new ServerSocket(65000+i, 50, InetAddress.getByName("127.0.0.1"));
                        System.out.println("Server is listening on port " + (65000+i));
                        break;
                    } catch (java.net.BindException e){
                        System.out.println("port " + (65000+i) + " failed, trying another");
                        i++;
                    }
                }

                System.out.println("(no-more-render) Started!");

                while (true) {
                    Socket socket = serverSocket.accept();

                    System.out.println("New client connected");

                    InputStream input = socket.getInputStream();
                    OutputStream output = socket.getOutputStream();

                    reader = new BufferedReader(new InputStreamReader(input));
                    writer = new PrintWriter(output, true);

//                    line = reader.readLine();

                    do {
                        line = reader.readLine();
                        parseCommand(line);

                    } while (!line.equals("bye"));

                    socket.close();

//                    reader.next
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        commandThread.setName("Headless Command Thread");
        commandThread.setDaemon(true);
        commandThread.start();
    }

    private void parseCommand(String command) {
        if(command == null) return;
        String[] split = command.split(" ");
        try {
            switch (split[0].toLowerCase()) {
                case "quit":
                case "exit":
                    self.stop();
                    break;
                case "listelements":
                case "elems":
                    listElements();
                    break;
                case "clickelement":
                case "celem":
                    clickElement(Integer.parseInt(split[1]));
                    break;
                case "writeelement":
                case "welem":
                    setElemText(Integer.parseInt(split[1]), command.substring(split[0].length()+split[1].length()+2));
                    break;
                case "key":
                    if (split.length > 1) {
                        simulateKeyPress(split[1]);
                    }
                    break;
                case "keydown":
                    if (split.length > 1) {
                        simulateKeyDown(split[1]);
                    }
                    break;
                case "keyup":
                    if (split.length > 1) {
                        simulateKeyUp(split[1]);
                    }
                    break;

                case "connect":
                    if (split.length == 1)
                        writer.println("You must specify a server address!");
                    if (split.length > 3)
                        writer.println("Too many arguments!");
                    else {

                        int port = 25565;

                        if (split.length == 3)
                            port = Integer.parseInt(split[2]);

                        writer.println("Connecting to: " + split[1] + ":" + port);
                        connect_to_serv(new ServerAddress(split[1], port));
                    }
                    break;
                case "chat":
                    assert self.player != null;
                    self.player.networkHandler.sendChatMessage(command.substring(5));
                    break;
                case "cmd":
                    assert self.player != null;
                    self.player.networkHandler.sendCommand(command.substring(4));
                    break;
                case "connectstatus":
                    if(self.player != null){
                        writer.println("connectstatus - Success");
                    }else{

                    }

                    break;
                default:
                    writer.println("Unknown command: " + split[0]);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void listElements() {
        Screen currentScreen = self.currentScreen;
        if (currentScreen == null) {
            writer.println("No screen is currently open.");
            return;
        }

        AtomicInteger index = new AtomicInteger(0);
        printElements(currentScreen.children(), 0, index);
    }


    private void printElements(List<? extends Element> elements, int depth, AtomicInteger index) {
        String indent = "  ".repeat(depth);
        for (Element element : elements) {
            writer.printf("%s%d: %s%n", indent, index.getAndIncrement(), describeElement(element));
            if (element instanceof ClickableWidget) {
                ClickableWidget widget = (ClickableWidget) element;
                writer.printf("%s   Message: %s%n", indent, widget.getMessage().getString());
            }
            if (element instanceof net.minecraft.client.gui.ParentElement pe) {
                printElements(pe.children(), depth + 1, index);
            } else if (element instanceof WorldListWidget wlw) {
                printElements(wlw.children(), depth+1, index);
            }// else if (element instanceof WorldListWidget.WorldEntry elw) {
//                printElements(elw.get, depth+1, index);
//            }
        }
    }

    private String describeElement(Element element) {
        return String.format("%s@%s", element.getClass().getSimpleName(), Integer.toHexString(element.hashCode()));
    }

    private Element findElement(int targetIndex) {
        Screen currentScreen = self.currentScreen;
        if (currentScreen == null) {
            writer.println("No screen is currently open.");
            return null;
        }

        AtomicInteger index = new AtomicInteger(0);
        return findElementByIndex(currentScreen.children(), targetIndex, index);
    }

    private void clickElement(int targetIndex) {
        Element targetElement = findElement(targetIndex);

        if (targetElement == null) {
            writer.println("Invalid element index.");
            return;
        }

        if (targetElement instanceof ClickableWidget widget) {
            self.execute(() -> {
                widget.onClick(widget.getX(), widget.getY());
            });
            writer.println("Clicked element: " + describeElement(widget));
            if (widget.getMessage() != null) {
                writer.println("Message: " + widget.getMessage().getString());
            }
        } else {
            writer.println("Element is not clickable: " + describeElement(targetElement));
        }
    }

    private void setElemText(int targetIndex, String text) {
        Element targetElement = findElement(targetIndex);

        if (targetElement == null) {
            writer.println("Invalid element index.");
            return;
        }



        if (targetElement instanceof TextFieldWidget widget) {
            self.execute(() -> {
                widget.setText("");
                widget.write(text);
            });
            writer.println("Wrote in element: " + describeElement(widget));
        } else {
            writer.println("Element is not a TextFieldWidget: " + describeElement(targetElement));
        }
    }

    private Element findElementByIndex(List<? extends Element> elements, int targetIndex, AtomicInteger currentIndex) {

        for (Element element : elements) {
            if (currentIndex.getAndIncrement() == targetIndex) {
                return element;
            }
            if (element instanceof net.minecraft.client.gui.ParentElement) {
                Element found = findElementByIndex(((net.minecraft.client.gui.ParentElement) element).children(), targetIndex, currentIndex);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

//    private String getMultiline() {
//
//
//        Screen currentScreen = self.currentScreen;
//        if (currentScreen == null) {
//            writer.println("No screen is currently open.");
//            return "";
//        }
//
//        for (Element element : currentScreen.children()) {
//
//            if (element instanceof MultilineTextWidget) {
//                Element found = findElementByIndex(((net.minecraft.client.gui.ParentElement) element).children(), targetIndex, currentIndex);
//                if (found != null) {
//                    return found;
//                }
//            }
//        }
//
//        return "";
//    }

    private void simulateKeyPress(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_PRESS, 0);
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_RELEASE, 0);
            });
            writer.println("Pressed key: " + keyName);
        } else {
            writer.println("Unknown key: " + keyName);
        }
    }

    private void simulateKeyDown(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_PRESS, 0);
            });
            writer.println("Key down: " + keyName);
        } else {
            writer.println("Unknown key: " + keyName);
        }
    }

    private void simulateKeyUp(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_RELEASE, 0);
            });
            writer.println("Key up: " + keyName);
        } else {
            writer.println("Unknown key: " + keyName);
        }
    }

    private static int getKeyCode(String keyName) {
        // Handle special cases
        switch (keyName.toLowerCase()) {
            case "space": return GLFW.GLFW_KEY_SPACE;
            case "enter": return GLFW.GLFW_KEY_ENTER;
            case "tab": return GLFW.GLFW_KEY_TAB;
            case "escape": return GLFW.GLFW_KEY_ESCAPE;
            case "backspace": return GLFW.GLFW_KEY_BACKSPACE;
//            case "rightclick": return GLFW.CLI;
//            case "leftclick": return GLFW.GLFW_KEY_BACKSPACE;
            // Add more special cases as needed
        }

        // For single characters, use their ASCII value
        if (keyName.length() == 1) {
            char c = Character.toUpperCase(keyName.charAt(0));
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        // If not found, return unknown key
        return GLFW.GLFW_KEY_UNKNOWN;
    }


    public void connect_to_serv(ServerAddress addr){
        self.execute(() -> {
            ConnectScreen.connect(self.currentScreen, self, addr, new ServerInfo("Test", addr.getAddress(), ServerInfo.ServerType.OTHER), true, null);
        });
    }

}
