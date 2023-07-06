import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientFrame {
    //GUI
    static JFrame frame;
    static JPanel chatArea;
    static JPanel controls;
    static JScrollPane scroll;
    static JButton sendText;
    static JButton sendFile;
    static JButton history;
    static JTextField textField;
    static JFileChooser fileChooser;

    //Client
    public static java.net.Socket socket;
    public static DataInputStream dIn;
    public static DataOutputStream dOut;
    public static String message;
    public static String selectText;
    public static int i;

    public static ArrayList<AudioInputStream> audioList;

    public ClientFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                runGUI();
            }
        });

        Thread clientThread = new Thread(() ->
        {
            System.out.println("Running on a separate Thread!");
            audioList = new ArrayList<AudioInputStream>();
            i = 0;
            selectText = "";
            try {
                socket = new Socket("localhost",5001);

                dIn = new DataInputStream(socket.getInputStream());
                dOut = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    String type = dIn.readUTF();
                    if (type.equals("text")) {
                        message = dIn.readUTF();
                        System.out.println("Server: "+message);
                        JLabel label = new JLabel("Server: "+message);
                        addToChat(label);
                        autoScroll();
                    } else if (type.equals("image")) {
                        int fileSize = dIn.readInt();
                        byte[] data = new byte[fileSize];
                        dIn.readFully(data);

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);

                        JLabel label = new JLabel(new ImageIcon(bufferedImage));
                        addToChat(label);

                        autoScroll();
//                        FileOutputStream fileOutputStream = new FileOutputStream("image.jpg");
//                        fileOutputStream.write(data);
//                        fileOutputStream.close();

                        System.out.println("Image Received");
                    } else if (type.equals("audio")) {
                        String audioName = dIn.readUTF();
                        int fileSize = dIn.readInt();
                        byte[] data = new byte[fileSize];
                        dIn.readFully(data);

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteArrayInputStream);

                        audioList.add(audioInputStream);
//                        clip.drain();

//                        addClickable();
                        addClickable(new JLabel(i+1 + " "+audioName));
                        i++;

                        autoScroll();
//                        addToChat(new JLabel("TESTTTTT"));

                    } else if (type.equals("selectresponse")) {
                        selectText = dIn.readUTF();
                        System.out.println("b se");
                        System.out.println(selectText);
                        System.out.println("a se");
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            }
        });

        clientThread.start();
    }

    private static void runGUI() {
        frame = new JFrame("okC");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));// Set the layout manager

        controls = new JPanel();

        sendText = new JButton("Send");

        sendText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JLabel label = new JLabel("YOU: " + textField.getText());
                try {
                    sendMessage(textField.getText());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                addToChat(label);
                textField.setText("");
                autoScroll();
            }
        });

        sendFile = new JButton("Send File");
        sendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("/home/mohsen"));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();

                    String filePath = selectedFile.getPath();
                    int dotIndex = filePath.lastIndexOf('.');
                    String ext = filePath.substring(dotIndex+1);
                    System.out.println(filePath);
                    System.out.println(ext);
                    if (ext.equals("png") || ext.equals("jpg")) {
                        try {
                            sendImage(filePath);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (ext.equals("wav") || ext.equals("mp3")) {
                        try {
                            sendAudio(filePath);
                        } catch (IOException | UnsupportedAudioFileException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    System.out.println("user cancelled");
                }
            }
        });

        history = new JButton("History");
        history.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("clicked");
                try {
                    askServerTOSelect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                JFrame hist = new JFrame("okA");
//                JLabel label = new JLabel("Hello\nWorld");
//                hist.add(label);
                TextArea textArea = new TextArea();
                System.out.println("before sele");
                System.out.println(selectText);
                System.out.println("after sele");
                textArea.setText(selectText);
                hist.add(textArea);
                hist.setPreferredSize(new Dimension(400,400));
                hist.pack();
                hist.setVisible(true);
            }
        });

        textField = new JTextField();
        textField.setPreferredSize(new Dimension(200,50));

        controls.add(textField);
        controls.add(sendText);
        controls.add(sendFile);
        controls.add(history);


            JLabel label = new JLabel("CLIENT..." );
            addToChat(label);

        // Create a JScrollPane and add the JPanel to it
        scroll = new JScrollPane(chatArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // scroll.setAutoscrolls(true);

        // Add chatArea and controls to the frame
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(1000,1000));
        frame.pack();
        frame.setVisible(true);
    }

    public static void addToChat(JLabel label) {
        chatArea.add(label);
        frame.revalidate();
        frame.repaint();

//        autoScroll();
    }

    public static void autoScroll() {
        JScrollBar verticalScroll = scroll.getVerticalScrollBar();
        verticalScroll.setValue(verticalScroll.getMaximum());
    }

    public static void addClickable(JLabel label) {
        MouseAdapter ma = new MouseAdapter() {
            Clip clip = null;
            boolean playToggle = false;
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (clip == null) {
                    try {
                        clip = AudioSystem.getClip();
                        int x = label.getText().charAt(0) - '1';
                        System.out.println(x);
                        clip.open(audioList.get(x));
                    } catch (LineUnavailableException | IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                System.out.println("mouse Clicked");

                if (!playToggle) {
                    clip.start();

                    System.out.println("Play audio");
                } else {
                    clip.flush();
                    clip.stop();;
                    System.out.println("Stop Audio");
                }
                playToggle = !playToggle;
            }
        };
        label.addMouseListener(ma);
        label.setForeground(Color.RED);
        chatArea.add(label);
        frame.revalidate();
        frame.repaint();

//        autoScroll();
    }

    public static void sendMessage(String text) throws IOException {
        dOut.writeUTF("text");
        dOut.writeUTF(text);
        dOut.flush();

        askServerToInsert(text);
    }

    private static void askServerToInsert(String text) throws IOException {
        dOut.writeUTF("sqlinsert");
        dOut.writeUTF(text);
    }

    private static void askServerTOSelect() throws IOException {
        dOut.writeUTF("sqlselect");
        dOut.flush();

        try {
            Thread.sleep(2000); // Wait for 5000 milliseconds (5 seconds)
        } catch (InterruptedException e) {
            // Handle interruption
        }
    }

    public static void sendImage(String imagePath) throws IOException {
        File image = new File(imagePath);
        byte[] data = new byte[(int)image.length()];

        try (FileInputStream fileInputStream = new FileInputStream(image)) {
            fileInputStream.read(data);
        }
        dOut.writeUTF("image");
        dOut.writeInt(data.length);
        dOut.write(data);
        dOut.flush();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);

        JLabel label = new JLabel(new ImageIcon(bufferedImage));
        addToChat(label);
        autoScroll();
    }

    public static void sendAudio(String audioPath) throws IOException, UnsupportedAudioFileException {
        File audio = new File(audioPath);

        byte[] data = new byte[(int)audio.length()];
        try (FileInputStream fileInputStream = new FileInputStream(audio)) {
            fileInputStream.read(data);
        }

        dOut.writeUTF("audio");
        dOut.writeUTF(audio.getName());
        dOut.writeInt(data.length);
        dOut.write(data);
        dOut.flush();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteArrayInputStream);

        audioList.add(audioInputStream);

        addClickable(new JLabel(i+1 + " " + audio.getName()));
        i++;

        autoScroll();
    }
}