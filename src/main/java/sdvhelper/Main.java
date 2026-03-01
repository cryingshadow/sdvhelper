package sdvhelper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Main {

    private static final Pattern PARTICIPANT_ID_PATTERN = Pattern.compile("\\d\\d?");

    private static final String START_MARKER = "#####";

    private static final Pattern TIME_PATTERN = Pattern.compile("\\d\\d:\\d\\d-\\d\\d:\\d\\d\\*?");

    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args == null || args.length != 3) {
            System.out.println("Call with seminar directory, commission member number, and year!");
            return;
        }
        final File root = new File(System.getProperty("user.dir"));
        final String seminarDirectoryName = args[0];
        final int memberIndex = Integer.parseInt(args[1]) - 1;
        final File directory = root.toPath().resolve(seminarDirectoryName).toFile();
        final File orgaTXT = Main.processOrgaFile(directory);
        final List<Examination> examinations = Main.parseExaminations(orgaTXT, directory, memberIndex);
        orgaTXT.delete();
        Main.writeNotes(directory, examinations, args[2]);
    }

    private static List<Examination> parseExaminations(
        final File orgaTXT,
        final File directory,
        final int memberIndex
    ) throws IOException {
        final List<Examination> examinations = new LinkedList<Examination>();
        try (final BufferedReader reader = new BufferedReader(new FileReader(orgaTXT))) {
            String line = reader.readLine();
            State state = State.NORMAL;
            int currentIndex = 0;
            String currentStart = "";
            String currentEnd = "";
            while (line != null) {
                if (!line.isBlank()) {
                    if (Main.TIME_PATTERN.matcher(line).matches()) {
                        final String[] parts = line.split("-");
                        currentStart = parts[0];
                        currentEnd = parts[1].endsWith("*") ? parts[1].substring(0, parts[1].length() - 1) : parts[1];
                        state = State.NORMAL;
                        currentIndex = 0;
                    } else {
                        switch (state) {
                        case NORMAL:
                            if (line.startsWith("Gruppenrunde")) {
                                state = State.GROUP;
                            } else if (line.startsWith("Einzelgespräch 1")) {
                                state = State.INTERVIEW1;
                            } else if (line.startsWith("Einzelgespräch 2")) {
                                state = State.INTERVIEW2;
                            }
                            break;
                        default:
                            if (Main.PARTICIPANT_ID_PATTERN.matcher(line).matches()) {
                                if (currentIndex == memberIndex) {
                                    final Examination examination =
                                        Main.toExamination(line, state, currentStart, currentEnd, directory);
                                    if (examination != null) {
                                        examinations.add(examination);
                                    }
                                }
                                currentIndex++;
                            } else {
                                state = State.NORMAL;
                                currentIndex = 0;
                            }
                        }
                    }
                }
                line = reader.readLine();
            }
        }
        return examinations;
    }

    private static String parseName(final String fileName) {
        final String[] parts = fileName.split("_");
        if (parts.length != 3) {
            System.out.println(String.format("Unexpected file name pattern for %s!", fileName));
        }
        return String.format("%s, %s", parts[1], parts[2].substring(0, parts[2].length() - 4));
    }

    private static File processOrgaFile(final File directory) throws IOException, InterruptedException {
        final File[] files =
            directory.listFiles(file -> file.getName().startsWith("Orgaplan") && file.getName().endsWith("pdf"));
        if (files.length != 1) {
            System.out.println("Could not determine orga file!");
            return null;
        }
        final File orgaPDF = files[0];
        final Process process = new ProcessBuilder(
            "pdftotext",
            orgaPDF.getName()
        ).inheritIO().directory(directory).start();
        process.waitFor(60, TimeUnit.SECONDS);
        return
            directory.toPath().resolve(orgaPDF.getName().substring(0, orgaPDF.getName().length() - 3) + "txt").toFile();
    }

    private static Examination toExamination(
        final String line,
        final State state,
        final String start,
        final String end,
        final File directory
    ) throws IOException {
        final int participantID = Integer.parseInt(line);
        final File[] files =
            directory.listFiles(file -> file.getName().startsWith(line + "_") && file.getName().endsWith("pdf"));
        if (files.length != 1) {
            System.out.println(String.format("Could not determine participant file for id %s!", line));
            return null;
        }
        final String name = Main.parseName(files[0].getName());
        return new Examination(participantID, state, name, start, end);
    }

    private static void writeGroupEntry(
        final int participantID,
        final String participantName,
        final String start,
        final List<Examination> groupExaminations,
        final BufferedWriter writer
    ) throws IOException {
        writer.write(Main.START_MARKER);
        writer.write(" ");
        writer.write(String.valueOf(participantID));
        writer.write(" G ");
        writer.write(participantName);
        writer.write(": \n\n");
        writer.write("Leistungsbereitschaft und Motivation: \n");
        writer.write("Leistungsbereitschaft: übernimmt Verantwortung für den Ablauf der Diskussion, hat sinnvolle ");
        writer.write("Diskussionsfragen vorbereitet \n");
        writer.write("Motivation: bringt neue Ideen und Lösungsvorschläge ein, nimmt Chancen wahr, leitet ");
        writer.write("Veränderungen ein,\n");
        writer.write("            beherrscht das Zeitmanagement \n");
        writer.write("Intellektuelle Fähigkeiten: \n");
        writer.write("Analytisches / systematisches Denken: durchdachter Vortrag, stringente Diskussionsbeiträge, ");
        writer.write("strukturiert\n");
        writer.write("                                      Beiträge anderer sinnvoll \n");
        writer.write("Vernetztes Denken: anschauliche Beispiele, sinnvolle Bezüge, zieht angemessenes Fazit \n");
        writer.write("Flexibilität / Kreativität / Offenheit: innovative Themenwahl, präsentiert neue Ideen und ");
        writer.write("Lösungsvorschläge,\n");
        writer.write("                                        ist offen für andere Sichtweisen, keine Wiederholungen ");
        writer.write("vorgebrachter Argumente \n");
        writer.write("Soziale Kompetenz: \n");
        writer.write("Dialogfähigkeit: kann eigenen Standpunkt begründen und ggf. adaptieren, würdigt Beiträge ");
        writer.write("anderer, ermuntert stille\n");
        writer.write("                 Teilnehmende, sich zu beteiligen, lässt andere zu Wort kommen, startet keine ");
        writer.write("Profilierungsversuche,\n");
        writer.write("                 versucht Konflikte zu schlichten, kann sich in die Sichtweisen von anderen ");
        writer.write("Personen hineinversetzen\n");
        writer.write("                 und das eigene Kommunikations- und Interaktionsverhalten auf andere ");
        writer.write("einstellen \n");
        writer.write("Sensibilität: zeigt Empathie und die Fähigkeit zum Perspektivwechsel, berücksichtigt ");
        writer.write("unterschiedliche sprachliche und\n");
        writer.write("              fachliche Voraussetzungen, nimmt verbale und nonverbale Signale der Gruppe ");
        writer.write("wahr, hört zu, fragt nach \n");
        writer.write("Kommunikations- und Artikulationsfähigkeit: \n");
        writer.write("Ausdrucksfähigkeit: spricht klar und verständlich, das Wesentliche wird auf den Punkt ");
        writer.write("gebracht, kann Sachverhalte gut darstellen \n");
        writer.write("Auftreten: zeigt durchgehend Präsenz, ist authentisch, bleibt gelassen in schwierigen ");
        writer.write("Situationen, geht respektvoll\n");
        writer.write("           mit anderen um, hält Blickkontakt \n\n");
        writer.write("Studiengang: \n\n");
        writer.write("Thema: \n\n");
        writer.write("Start: ");
        writer.write(start);
        writer.write("\n\n");
        writer.write("Ende Vortrag: \n\n");
        writer.write("Ende Diskussion: \n\n");
        for (final Examination examination : groupExaminations) {
            if (examination.id() == participantID) {
                continue;
            }
            writer.write("Teilnehmer ");
            writer.write(String.valueOf(examination.id()));
            writer.write(" ");
            writer.write(examination.name());
            writer.write(":\n\n\n");
        }
        writer.write("\n\n");
    }

    private static void writeInterviewEntry(
        final String interviewForm,
        final int participantID,
        final String participantName,
        final String year,
        final String start,
        final String end,
        final BufferedWriter writer
    ) throws IOException {
        writer.write(start);
        writer.write("-");
        writer.write(end);
        writer.write("\n");
        writer.write(Main.START_MARKER);
        writer.write(" ");
        writer.write(String.valueOf(participantID));
        writer.write(" ");
        writer.write(interviewForm);
        writer.write(" ");
        writer.write(participantName);
        writer.write(": \n\n");
        writer.write("Geburtsjahr: \n");
        writer.write("Studiengang: \n");
        writer.write("Abi: \n");
        writer.write("Studium aktuell: \n");
        writer.write("WS ");
        writer.write(year);
        writer.write(": \n");
        writer.write("Alternativen: \n");
        writer.write("Wunsch: \n");
        writer.write("Leistungen: \n\n");
        writer.write("Hürden: \n\n");
        writer.write("Interessen: \n\n");
        writer.write("Sonstiges: \n\n\n");
        writer.write("Engagement und Interessen: \n");
        writer.write("Engagement: setzt sich nachhaltig und ausdauernd für andere ein, übernimmt Verantwortung \n");
        writer.write("Interessen: intensive, reflektierte und ausdauernde Beschäftigung mit Gegenständen außerhalb ");
        writer.write("des eigenen Curriculums \n");
        writer.write("- \n");
        writer.write("- \n\n");
        writer.write("Leistungsbereitschaft und Motivation: \n");
        writer.write("Leistungsbereitschaft: steckt sich hohe Ziele, arbeitet hoch motiviert, ist einsatzfreudig ");
        writer.write("und fähig, sich zu organisieren \n");
        writer.write("Motivation: nimmt Chancen wahr, leitet selbst Veränderungen ein, verfolgt Ziele und ");
        writer.write("Initiativen aus eigenem Antrieb \n");
        writer.write("Ausdauer: verfolgt hochgesteckte Ziele ausdauernd und geht mit Hindernissen erfolgreich um \n");
        writer.write("- \n");
        writer.write("- \n\n");
        writer.write("Intellektuelle Fähigkeiten: \n");
        writer.write("Analytische und systematische Problemlösung: strukturiert Probleme logisch und fokussiert ");
        writer.write("auf relevante Aspekte \n");
        writer.write("Vernetztes Denken: zeigt Neugier, ist fähig zu kombinieren (zum Beispiel Herleiten von ");
        writer.write("Analogien, Erstellen einer Synthese) \n");
        writer.write("Flexibilität / Kreativität: ist offen für neue Sichtweisen und entwickelt eigenständig neue ");
        writer.write("Ideen \n");
        writer.write("- \n");
        writer.write("- \n\n");
        writer.write("Soziale Kompetenz: \n");
        writer.write("Dialogfähigkeit: zeigt in bisherigen Engagements lösungsorientiertes soziales Verhalten und ");
        writer.write("arbeitet konstruktiv mit\n");
        writer.write("                 anderen zusammen, kann sich in die Sichtweise von anderen Personen ");
        writer.write("hineinversetzen, kann eigenes\n");
        writer.write("                 Kommunikations- und Interaktionsverhalten auf andere einstellen, im Dialog ");
        writer.write("den eigenen Standpunkt\n");
        writer.write("                 begründen und ggf. adaptieren \n");
        writer.write("Offenheit und Sensibilität: zeigt sich gegenüber neuem (sozialem oder kulturellem) Umfeld ");
        writer.write("offen, nimmt Anregungen auf, zeigt Empathie \n");
        writer.write("- \n");
        writer.write("- \n\n");
        writer.write("Kommunikations- und Artikulationsfähigkeit: \n");
        writer.write("Ausdrucksfähigkeit: drückt sich korrekt aus und findet die richtigen Worte \n");
        writer.write("Auftreten: zeigt Selbstbewusstsein und Präsenz ohne arrogant zu wirken, ist authentisch, ");
        writer.write("hält Blickkontakt \n");
        writer.write("- \n\n\n\n\n");
    }

    private static void writeNotes(
        final File directory,
        final List<Examination> examinations,
        final String year
    ) throws IOException {
        final File notes = directory.toPath().resolve("notizen.txt").toFile();
        final List<Examination> groupExaminations =
            examinations.stream().filter(e -> e.state() == State.GROUP).toList();
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(notes))) {
            for (final Examination examination : examinations) {
                switch (examination.state()) {
                case GROUP:
                    Main.writeGroupEntry(
                        examination.id(),
                        examination.name(),
                        examination.start(),
                        groupExaminations,
                        writer
                    );
                    break;
                case NORMAL:
                    System.out.println(
                        String.format(
                            "Could not determine examination for ID %d and name %s!",
                            examination.id(),
                            examination.name()
                        )
                    );
                    break;
                default:
                    Main.writeInterviewEntry(
                        examination.state().id,
                        examination.id(),
                        examination.name(),
                        year,
                        examination.start(),
                        examination.end(),
                        writer
                    );
                }
            }
        }
    }

}
