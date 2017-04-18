package crw.ui.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Plot the path a boat traveled from its log data
 *
 * @author nbb
 */
public class LogAnalyzer {

    HashMap<String, Integer> phraseToCount = new HashMap<String, Integer>();
    String placeLeft;

    public LogAnalyzer() {
    }

    public boolean loadDirectory(String path) {
        return loadDirectory(path, null);
    }

    public boolean loadDirectory(String path, final String[] fileNameInclusions) {
        File directory = new File(path);
        if (directory != null && directory.isDirectory()) {

            File[] files = directory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (!name.toLowerCase().endsWith(".txt")) {
                        return false;
                    }
                    if (fileNameInclusions != null) {
                        for (String s : fileNameInclusions) {
                            if (!name.contains(s)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });
            System.out.println("Found " + files.length + " files");
            for (File file : files) {
                loadFile(file.getAbsolutePath());
            }
        }

        return true;
    }

    public boolean loadDirectory(String path, final String[] directoryNameExclusions, final String[] fileNameInclusions) {
        System.out.println("Loading team directory: " + path);

        File directory = new File(path);
        if (directory != null && directory.isDirectory()) {

            File[] subDirectories = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        return false;
                    }
                    if (directoryNameExclusions != null) {
                        for (String s : directoryNameExclusions) {
                            if (file.getName().contains(s)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });

            for (File subDirectory : subDirectories) {
                loadDirectory(subDirectory.getAbsolutePath(), fileNameInclusions);
            }
        }
        return true;
    }

    public boolean loadFile(String path) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd:HH:mm:ss");
        File file = new File(path);
        if (file != null && file.isFile()) {
            Scanner input;
            try {
                input = new Scanner(file);

                System.out.println("Loading " + path);

                while (input.hasNext()) {
                    String nextLine = input.nextLine();
                    parseLine(nextLine);
                }
                input.close();

            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

        } else {
            System.out.println("Could not load file " + path);
        }

        return true;
    }

    public void parseLine(String nextLine) {

        StringTokenizer st = new StringTokenizer(nextLine, " ");
        try {
            if (nextLine.contains("Spawning root mission from spec")) {
//INFO 1450345280809 sami.engine.Engine.spawnRootMission (1489) "Spawning root mission from spec [Connect Boat]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("[");
                int i2 = remainder.indexOf("]");
                String planName = remainder.substring(i1 + 1, i2);
                if (planName.length() == 0) {
                    return;
                }
                System.out.println("" + planName);
            } else if (nextLine.contains("Spawning child mission from spec")) {
//INFO 1450345564029 sami.engine.Engine.spawnSubMission (1489) "Spawning child mission from spec [recover.SK 2DOF] with parent tokens [[Token:P-8-NULL]]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("[");
                int i2 = remainder.indexOf("]");
                String planName = remainder.substring(i1 + 1, i2);
                if (planName.length() == 0) {
                    return;
                }
                System.out.println("" + planName);
            } else if (nextLine.contains("Creating PlanManager for mSpec")) {
//INFO 1450345280841 sami.engine.PlanManager.<init> (1489) "Creating PlanManager for mSpec Connect Boat with mission ID 2757885b-1476-4dd3-b5f4-fc466f9baa27 and planName Connect Boat"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("Creating PlanManager for ");
                int i2 = remainder.indexOf(" with mission ID");
                String planName = remainder.substring(i1 + 25, i2);
                if (planName.length() == 0) {
                    return;
                }
                System.out.println("" + planName);
            } else if (nextLine.contains("Entering Place")) {
//INFO 1450345280889 sami.engine.PlanManager.enterPlace (1489) "Entering Place: with Tokens: [Token:Generic] with checkForTransition: true, getInTransitions: [], inEdges: [], getOutTransitions: [Transition:, Transition:AbortMissionHelper, Transition:CompleteMissionReceivedHelper], outEdges: [InEdge, InEdge, InEdge]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("Entering Place:");
                int i2 = remainder.indexOf(" with Tokens:");
                String placeName = remainder.substring(i1 + 15, i2);
                if (placeName.length() == 0) {
                    return;
                }
                System.out.println("" + placeName);
            } else if (nextLine.contains("Executing Transition")) {
//INFO 1450345287255 sami.engine.PlanManager.executeTransition (1489) "Executing Transition:, inPlaces: [Place:], inEdges: [InEdge], outPlaces: [Place:], outEdges: [OutEdge]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String skip = st.nextToken("(1489)");
                String remainder = st.nextToken();
                int i1 = remainder.indexOf("Executing Transition:");
                int i2 = remainder.indexOf(", inPlaces:");
                String transitionName = remainder.substring(i1 + 21, i2);
                if (transitionName.length() == 0) {
                    return;
                }
                System.out.println("" + transitionName);
            } else if (nextLine.contains("Leaving Place")) {
//INFO 1450345287255 sami.engine.PlanManager.leavePlace (1489) "Leaving Place: with [Token:Generic] and taking []"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("Leaving Place:");
                int i2 = remainder.indexOf(" with [");
                String placeName = remainder.substring(i1 + 14, i2);
                if (placeName.length() == 0) {
                    return;
                }
                System.out.println("" + placeName);
            } else if (nextLine.contains("Finishing plan")) {
//INFO 1450345288344 sami.engine.PlanManager.reachedEndPlace (1489) "Finishing plan [Connect Boat] at end place [Place:]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("[");
                int i2 = remainder.indexOf("]");
                String planName = remainder.substring(i1 + 1, i2);
                int i3 = remainder.substring(i2 + 1).indexOf("[");
                int i4 = remainder.substring(i2 + 1).indexOf("]");
                String placeName = remainder.substring(i2 + 1).substring(i3 + 1, i4);
                if (planName.length() == 0) {
                    return;
                }
                System.out.println("" + planName + "\t" + placeName);
            } else if (nextLine.contains("Complete Mission called with mission id")) {
//INFO 1450345288351 crw.proxy.BoatProxy.completeMission (1489) "Complete Mission called with mission id [2757885b-1476-4dd3-b5f4-fc466f9baa27] on proxy [GRN SQ]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String skip = st.nextToken("(1489)");
                String remainder = st.nextToken();
                //
            } else if (nextLine.contains("MessagePanel ToUiMessage received")) {
//INFO 1450345288343 crw.ui.MessagePanel.toUiMessageReceived (1489) "MessagePanel ToUiMessage received: InformationMessage [Conencted]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("MessagePanel ToUiMessage received: ");
//                            int i2 = remainder.indexOf("]");
                String message = remainder.substring(i1 + 35);
                if (message.length() == 0) {
                    return;
                }
                System.out.println("" + message);
            } else if (nextLine.contains("toUiMessageReceived")) {
//INFO 1450345280896 sami.uilanguage.LocalUiClientServer.toUiMessageReceived (34) "toUiMessageReceived: SelectionMessage [false, false, true, [BoatProxyId [YLW CRC, java.awt.Color[r=255,g=246,b=0], 192.168.1.123:11411, ""], BoatProxyId [GRN TRI, java.awt.Color[r=10,g=255,b=0], 192.168.1.118:11411, ""], BoatProxyId [BLK DM, java.awt.Color[r=40,g=0,b=0], 192.168.1.114:11411, ""], BoatProxyId [ORG TRI, java.awt.Color[r=255,g=202,b=10], 192.168.1.117:11411, ""], BoatProxyId [ORG DM, java.awt.Color[r=255,g=206,b=0], 192.168.1.110:11411, ""], BoatProxyId [RED TRI, java.awt.Color[r=255,g=0,b=10], 192.168.1.119:11411, ""], BoatProxyId [YLW SQ, java.awt.Color[r=255,g=232,b=0], 192.168.1.102:11411, ""], BoatProxyId [BLK SQ, java.awt.Color[r=20,g=0,b=0], 192.168.1.107:11411, ""], BoatProxyId [RED DM, java.awt.Color[r=244,g=0,b=0], 192.168.1.112:11411, ""], BoatProxyId [WHT DM, java.awt.Color[r=255,g=255,b=255], 192.168.1.108:11411, ""], BoatProxyId [BLU DM, java.awt.Color[r=0,g=132,b=255], 192.168.1.113:11411, ""], BoatProxyId [GRN DM, java.awt.Color[r=0,g=255,b=10], 192.168.1.111:11411, ""], BoatProxyId [WHT CRC, java.awt.Color[r=255,g=255,b=255], 192.168.1.122:11411, ""], BoatProxyId [RED SQ, java.awt.Color[r=255,g=0,b=10], 192.168.1.105:11411, ""], BoatProxyId [ORG SQ, java.awt.Color[r=255,g=202,b=0], 192.168.1.103:11411, ""], BoatProxyId [BLU SQ, java.awt.Color[r=0,g=10,b=255], 192.168.1.106:11411, ""], BoatProxyId [WHT TRI, java.awt.Color[r=255,g=255,b=255], 192.168.1.115:11411, ""], BoatProxyId [BLK TRI, java.awt.Color[r=20,g=0,b=0], 192.168.1.121:11411, ""], BoatProxyId [BLU TRI, java.awt.Color[r=0,g=132,b=255], 192.168.1.120:11411, ""], BoatProxyId [WHT SQ, java.awt.Color[r=255,g=255,b=255], 192.168.1.101:11411, ""], BoatProxyId [YLW TRI, java.awt.Color[r=255,g=237,b=0], 192.168.1.116:11411, ""], BoatProxyId [GRN SQ, java.awt.Color[r=91,g=255,b=0], 192.168.1.104:11411, ""], BoatProxyId [GRN CRC, java.awt.Color[r=0,g=255,b=10], 192.168.1.125:11411, ""], BoatProxyId [YLW DM, java.awt.Color[r=255,g=237,b=0], 192.168.1.109:11411, ""], BoatProxyId [ORG CRC, java.awt.Color[r=255,g=206,b=0], 192.168.1.124:11411, ""]]]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(34)") + 4);
                int i1 = remainder.indexOf("toUiMessageReceived: ");
                int i2 = remainder.indexOf(" [");
                String message;
                if (i2 != -1) {
                    message = remainder.substring(i1 + 21, i2);
                } else {
                    message = remainder.substring(i1 + 21);
                }
                if (message.length() == 0) {
                    return;
                }
                System.out.println("" + message);
            } else if (nextLine.contains("Message from UI")) {
//INFO 1450345287253 sami.uilanguage.LocalUiClientServer.UIMessage (63) "Message from UI: BoatIdSelectedMessage [BoatProxyId [GRN SQ, java.awt.Color[r=91,g=255,b=0], 192.168.1.104:11411, ""]]"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(63)") + 4);
                int i1 = remainder.indexOf("Message from UI: ");
                int i2 = remainder.indexOf(" [");
                String message = remainder.substring(i1 + 17, i2);
                if (message.length() == 0) {
                    return;
                }
                System.out.println("" + message);
            } else if (nextLine.contains("Message list updated")) {
//INFO 1450345288640 crw.ui.MessagePanel.getMessageArray (1489) "Message list updated; 0 critical 0 high 0 medium 0 low "
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("Message list updated; ");
//                            int i2 = remainder.indexOf(" [");
                String message = remainder.substring(i1 + 22);
                if (message.length() == 0) {
                    return;
                }
                System.out.println("" + message);
            } else if (nextLine.contains("Boat proxy created with name")) {
//INFO 1450345287259 crw.proxy.BoatProxy.<init> (1489) "Boat proxy created with name: GRN SQ, color: java.awt.Color[r=91,g=255,b=0], addr: /192.168.1.104:11411"
                st.nextToken();
                long epochTime = Long.parseLong(st.nextToken());
                String remainder = nextLine.substring(nextLine.indexOf("(1489)") + 7);
                int i1 = remainder.indexOf("Boat proxy created with name: ");
                int i2 = remainder.indexOf(", color:");
                String boat = remainder.substring(i1 + 30, i2);
                if (boat.length() == 0) {
                    return;
                }
                System.out.println("" + boat);
            }
        } catch (NoSuchElementException nsee) {

        }
    }

    public void clearWidgets() {

    }

    public static void main(String[] args) {
        final LogAnalyzer logAnalyzer = new LogAnalyzer();

////        String nextLine = "INFO 1450345280809 sami.engine.Engine.spawnRootMission (1489) \"Spawning root mission from spec [Connect Boat]\"";
////        String nextLine = "INFO 1450345288344 sami.engine.PlanManager.reachedEndPlace (1489) \"Finishing plan [Connect Boat] at end place [Place:]\"";
//        String nextLine = "INFO 1450339660357 sami.engine.PlanManager.reachedEndPlace (1489) \"Finishing plan [recover.SK 2DOF9] at end place [Place:AbortMissionHelper]\"";
//        logAnalyzer.parseLine(nextLine);
        
//
//        logAnalyzer.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/mac/", new String[]{"log"});
//        logAnalyzer.loadDirectory("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/mac/", new String[]{"log-12-17-15"});
        logAnalyzer.loadFile("/Users/nbb/Documents/cmu/phd/airboats/cmuq-2015-dec/mac/log-12-17-15-1.txt");

        System.out.println("Done loading");
    }
}
