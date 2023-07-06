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
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Statement;
import java.sql.ResultSet;

public class ServerFrame {
    //GUI
    static JFrame frame;
    static JPanel chatArea;
    static JPanel controls;
    static JScrollPane scroll;
    static JButton sendText;
    static JButton sendFile;
    static JTextField textField;
    static JFileChooser fileChooser;

    //Server
    public static ServerSocket server;
    public static Socket socket;
    public static DataInputStream dIn;
    public static DataOutputStream dOut;

    public static String message;
    public static int i;
    public static ArrayList<AudioInputStream> audioList;

    public ServerFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                runGUI();
            }
        });

        Thread serverThread = new Thread(() ->
        {
            System.out.println("Running on a separate Thread!");
            audioList = new ArrayList<AudioInputStream>();
            i=0;

            try {
                SqlConnect.ConnectToSQL();
                System.out.println("Conncetd");

            } catch (Exception e) {
                System.out.println("Did not Connect");
            }

            try {
                server = new ServerSocket(5001);
                socket = server.accept();

                System.out.println(socket.getInetAddress());

                dIn = new DataInputStream(socket.getInputStream());
                dOut = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    String type = dIn.readUTF();
                    if (type.equals("text")) {
                        message = dIn.readUTF();
                        System.out.println("Client: "+message);
                        JLabel label = new JLabel("Client: "+message);
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
                    } else if (type.equals("sqlinsert")) {
                        String txt = dIn.readUTF();
                        insertClinetToDB(txt);
                    } else if (type.equals("sqlselect")) {
                        String txt = sqlSelect();
                        System.out.println(txt);
                        dOut.writeUTF("selectresponse");
                        dOut.writeUTF(txt);
                        dOut.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                throw new RuntimeException(e);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        serverThread.start();
    }

    private static void runGUI() {
        frame = new JFrame("okB");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a JPanel with a larger preferred size than the container
        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));// Set the layout manager
//        chatArea.setPreferredSize(new Dimension(1000,1000));
//        JPanel up = new JPanel();
//        up.setLayout(new BoxLayout(up,BoxLayout.X_AXIS));
//        up.setPreferredSize(new Dimension(1000,50));
        // Create a separate panel for the button
        controls = new JPanel();
//        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
//        controls.setPreferredSize(new Dimension(1000,100));
        sendText = new JButton("Send");

//        sendText.setPreferredSize(new Dimension(200,80));
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
//        System.out.println(sendText.getSize());

        textField = new JTextField();
        textField.setPreferredSize(new Dimension(200,50));

        controls.add(textField);
        controls.add(sendText);
        controls.add(sendFile);

//        JLabel lb = new JLabel("...Chat Area...");
//        lb.setFont(new Font("FiraCode Nerd Font", Font.ITALIC,Font.BOLD));
//        up.add(lb);
        // Add components to the chatArea

        JLabel label = new JLabel("SERVER...");
        addToChat(label);

        // Create a JScrollPane and add the JPanel to it
        scroll = new JScrollPane(chatArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // scroll.setAutoscrolls(true);

        // Add chatArea and controls to the frame
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(controls, BorderLayout.SOUTH);
        frame.setPreferredSize(new Dimension(1000,1000));
//        frame.setTitle("okC");
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

    public static String sqlSelect() throws SQLException {
        String sql = "SELECT * FROM MESSAGES";
        Statement st = SqlConnect.con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        String list = "";
        while(rs.next()){
            //String str_crs_C_id = String.valueOf(rs.getInt("c_id"));
            String id = String.valueOf(rs.getInt("Id"));
            String sender = rs.getString("Sender");
            String message = rs.getString("Message");
            list = list.concat(id+ " "+ sender + ": " + message+"\n");
        }
//        System.out.println(list);
        System.out.println("Selected: ");
        return list;
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

//        insertToDB(text);
        insertServerToDB(text);
    }

    public static void insertClinetToDB(String text) {
        String statement = "INSERT INTO MESSAGES VALUES ('Client', '" +text+"')";
        System.out.println(statement);
        SqlConnect.executeNonquary(statement);
        System.out.println("Inserted");
    }
    public static void insertServerToDB(String text) {
        String statement = "INSERT INTO MESSAGES VALUES ('Server', '" +text+"')";
        System.out.println(statement);
        SqlConnect.executeNonquary(statement);
        System.out.println("Inserted");
    }
//    INSERT INTO MESSAGES VALUES ('Client', 'Hello');
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


//    Play Icon
//    <a href="https://www.flaticon.com/free-icons/play-button" title="play button icons">Play button icons created by Noplubery - Flaticon</a>
}
