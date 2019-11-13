package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to ferry global state from an Instance object into JSON.
 */
public class GlobalState {

    @JsonView(Views.Public.class)
    private ArrayList<EClass> classes;
    @JsonView(Views.Public.class)
    private ArrayList<User> users;
    @JsonView(Views.Public.class)
    private ArrayList<Student> students;
    @JsonView(Views.Public.class)
    private ArrayList<UMessage> messages;
    @JsonView(Views.Public.class)
    private String token;

    public GlobalState() {}

    private static void addIfAbsent(User u, Collection<User> destination, Set<User> known) {
        if (known.add(u)) {
            destination.add(u);
        }
    }

    private static void addIfAbsent(Collection<User> source, Collection<User> destination, Set<User> known) {
        for (User u : source) {
            addIfAbsent(u, destination, known);
        }
    }

    /**
     * Creates a personalized view of an instance for a user.
     * Admin users see everything.
     * Teachers & Guardians only see themselves in full detail, and others in abridged format.
     * Guardians cannot see students that they are not guardians for.
     *
     * @param token identifiying both instance and user to look as
     */
    public GlobalState(Token token) {
        // later ease of reference
        User u = token.getUser();
        Instance i = u.getInstance();

        // everybody can see their own messages
        messages = new ArrayList<>(u.getSent());
        messages.addAll(u.getReceived());

        // fill in token field
        this.token = token.getKey();

        // fill in visible classes, users, students
        classes = new ArrayList<>();
        users = new ArrayList<>();
        students = new ArrayList<>();
        if (u.hasRole(User.Role.ADMIN)) {
            // see everybody
            classes.addAll(i.getClasses());
            users.addAll(i.getUsers());
            students.addAll(i.getStudents());
        } else {
            HashSet<User> uniqueUsers = new HashSet<>();
            if (u.hasRole(User.Role.TEACHER)) {
                // see own classes & students -- and their guardians
                HashSet<Student> uniqueStudents = new HashSet<>();
                for (EClass c : i.getClasses()) {
                    if (c.getTeachers().contains(u)) {
                        classes.add(c);
                        addIfAbsent(c.getTeachers(), users, uniqueUsers);
                        for (Student s : c.getStudents()) {
                            if (uniqueStudents.add(s)) {
                                students.add(s);
                                addIfAbsent(s.getGuardians(), users, uniqueUsers);
                            }
                        }
                    }
                }
            } else if (u.hasRole(User.Role.GUARDIAN)) {
                // see guarded, their teachers -- and nobody else
                for (Student s: i.getStudents()) {
                    students.add(s);
                    if (s.getGuardians().contains(u)) {
                        addIfAbsent(s.getEClass().getTeachers(), users, uniqueUsers);
                    }
                }
            }
        }
        // filter out class users: those are implicit
        users.removeIf(o -> o.hasRole(User.Role.CLASS));
    }
}
