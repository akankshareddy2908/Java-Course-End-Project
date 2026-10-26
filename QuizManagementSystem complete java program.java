import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class QuizManagementSystem extends JFrame {
    Connection con;
    JTextField id, question, a, b, c, d, correct;
    JTextField subject, difficulty, marks, search;
    JTable table;
    DefaultTableModel model;

    QuizManagementSystem() {
        setTitle("Quiz and Question Bank Management System");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/quizdb", "root", "root");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Database connection failed: " + e.getMessage());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));

        id = new JTextField();
        question = new JTextField();
        a = new JTextField();
        b = new JTextField();
        c = new JTextField();
        d = new JTextField();
        correct = new JTextField();
        subject = new JTextField();
        difficulty = new JTextField();
        marks = new JTextField();
        search = new JTextField();

        addField(form, "Question ID (for update/delete)", id);
        addField(form, "Question", question);
        addField(form, "Option A", a);
        addField(form, "Option B", b);
        addField(form, "Option C", c);
        addField(form, "Option D", d);
        addField(form, "Correct Option (A/B/C/D)", correct);
        addField(form, "Subject", subject);
        addField(form, "Difficulty", difficulty);
        addField(form, "Marks", marks);

        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");
        JButton find = new JButton("Search");
        JButton quiz = new JButton("Start Quiz");

        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);
        buttons.add(quiz);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search by subject:"));
        searchPanel.add(search);
        searchPanel.add(find);

        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{
            "ID", "Question", "A", "B", "C", "D",
            "Answer", "Subject", "Difficulty", "Marks"
        });

        table = new JTable(model);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(searchPanel, BorderLayout.SOUTH);

        add.addActionListener(e -> addQuestion());
        update.addActionListener(e -> updateQuestion());
        delete.addActionListener(e -> deleteQuestion());
        clear.addActionListener(e -> clearFields());
        find.addActionListener(e -> searchQuestions());
        quiz.addActionListener(e -> startQuiz());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.getSelectedRow();
                id.setText(model.getValueAt(r, 0).toString());
                question.setText(model.getValueAt(r, 1).toString());
                a.setText(model.getValueAt(r, 2).toString());
                b.setText(model.getValueAt(r, 3).toString());
                c.setText(model.getValueAt(r, 4).toString());
                d.setText(model.getValueAt(r, 5).toString());
                correct.setText(model.getValueAt(r, 6).toString());
                subject.setText(model.getValueAt(r, 7).toString());
                difficulty.setText(model.getValueAt(r, 8).toString());
                marks.setText(model.getValueAt(r, 9).toString());
            }
        });

        loadQuestions();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    void addField(JPanel p, String label, JTextField field) {
        p.add(new JLabel(label));
        p.add(field);
    }

    boolean validInput() {
        try {
            if (question.getText().trim().isEmpty()
                || a.getText().trim().isEmpty()
                || b.getText().trim().isEmpty()
                || c.getText().trim().isEmpty()
                || d.getText().trim().isEmpty()
                || subject.getText().trim().isEmpty()
                || difficulty.getText().trim().isEmpty()) {
                throw new Exception("Fill in all question details.");
            }

            String ans = correct.getText().trim().toUpperCase();
            if (!ans.matches("[ABCD]"))
                throw new Exception("Correct option must be A, B, C or D.");

            if (Integer.parseInt(marks.getText()) <= 0)
                throw new Exception("Marks must be positive.");

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return false;
        }
    }

    void addQuestion() {
        if (con == null || !validInput()) return;

        String sql = "INSERT INTO questions " +
            "(question_text, option_a, option_b, option_c, option_d, " +
            "correct_option, subject, difficulty, marks) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            fillStatement(ps);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Question added!");
            loadQuestions();
            clearFields();
        } catch (Exception e) {
            showError(e);
        }
    }

    void fillStatement(PreparedStatement ps) throws SQLException {
        ps.setString(1, question.getText().trim());
        ps.setString(2, a.getText().trim());
        ps.setString(3, b.getText().trim());
        ps.setString(4, c.getText().trim());
        ps.setString(5, d.getText().trim());
        ps.setString(6, correct.getText().trim().toUpperCase());
        ps.setString(7, subject.getText().trim());
        ps.setString(8, difficulty.getText().trim());
        ps.setInt(9, Integer.parseInt(marks.getText()));
    }

    void updateQuestion() {
        if (con == null || !validInput()) return;

        try (PreparedStatement ps = con.prepareStatement(
            "UPDATE questions SET question_text=?, option_a=?, option_b=?," +
            " option_c=?, option_d=?, correct_option=?, subject=?," +
            " difficulty=?, marks=? WHERE id=?")) {
            fillStatement(ps);
            ps.setInt(10, Integer.parseInt(id.getText()));
            int n = ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                n > 0 ? "Question updated!" : "Question ID not found.");
            loadQuestions();
        } catch (Exception e) {
            showError(e);
        }
    }

    void deleteQuestion() {
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
            "DELETE FROM questions WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(id.getText()));
            int n = ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                n > 0 ? "Question deleted!" : "Question ID not found.");
            loadQuestions();
            clearFields();
        } catch (Exception e) {
            showError(e);
        }
    }

    void loadQuestions() {
        if (con == null) return;
        model.setRowCount(0);

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM questions")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("question_text"),
                    rs.getString("option_a"), rs.getString("option_b"),
                    rs.getString("option_c"), rs.getString("option_d"),
                    rs.getString("correct_option"), rs.getString("subject"),
                    rs.getString("difficulty"), rs.getInt("marks")
                });
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    void searchQuestions() {
        if (con == null) return;
        model.setRowCount(0);

        try (PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM questions WHERE subject LIKE ?")) {
            ps.setString(1, "%" + search.getText().trim() + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("question_text"),
                        rs.getString("option_a"), rs.getString("option_b"),
                        rs.getString("option_c"), rs.getString("option_d"),
                        rs.getString("correct_option"), rs.getString("subject"),
                        rs.getString("difficulty"), rs.getInt("marks")
                    });
                }
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    void startQuiz() {
        if (con == null) return;

        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String[]> questions = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM questions")) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
                questions.add(new String[]{
                    rs.getString("question_text"),
                    rs.getString("option_a"),
                    rs.getString("option_b"),
                    rs.getString("option_c"),
                    rs.getString("option_d"),
                    rs.getString("correct_option"),
                    String.valueOf(rs.getInt("marks"))
                });
            }
        } catch (Exception e) {
            showError(e);
            return;
        }

        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No questions available.");
            return;
        }

        int score = 0, total = 0;

        for (String[] q : questions) {
            String[] options = {
                "A. " + q[1], "B. " + q[2],
                "C. " + q[3], "D. " + q[4]
            };

            int choice = JOptionPane.showOptionDialog(
                this, q[0], "Quiz",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

            if (choice == -1) break;

            total += Integer.parseInt(q[6]);

            if ("ABCD".charAt(choice) == q[5].charAt(0))
                score += Integer.parseInt(q[6]);
        }

        JOptionPane.showMessageDialog(this,
            "Your Score: " + score + " / " + total);
    }

    void clearFields() {
        id.setText("");
        question.setText("");
        a.setText("");
        b.setText("");
        c.setText("");
        d.setText("");
        correct.setText("");
        subject.setText("");
        difficulty.setText("");
        marks.setText("");
    }

    void showError(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizManagementSystem());
    }
}