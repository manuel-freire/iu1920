package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to ferry global state from an Instance object into JSON.
 */
public class GlobalState {

    private ArrayList<EClass> classes;
    private ArrayList<User> users;
    private ArrayList<Student> students;
    private ArrayList<UMessage> messages;

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
     * @param instance to look into
     * @param user to look as. Null sees everything as an admin.
     *             YES, THIS IS JUST A DEMO APP AND SECURITY IS NON-EXISTENT
     */
    public GlobalState(Instance instance, User user) {
        // everybody can see their own messages
        messages = new ArrayList(user.getSent());
        messages.addAll(user.getReceived());

        classes = new ArrayList<>();
        users = new ArrayList<>();
        students = new ArrayList<>();
        if (user == null || user.hasRole(User.Role.ADMIN)) {
            // see everybody
            classes.addAll(instance.getClasses());
            users.addAll(instance.getUsers());
            students.addAll(instance.getStudents());
        } else if (user.hasRole(User.Role.TEACHER)) {
            // see own classes & students
            HashSet<Student> uniqueStudents = new HashSet<>();
            HashSet<User> uniqueUsers = new HashSet<>();
            for (EClass c : instance.getClasses()) {
                if (c.getTeachers().contains(user)) {
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
        } else if (user.hasRole(User.Role.GUARDIAN)) {
            // see guarded, their teachers
            HashSet<User> uniqueUsers = new HashSet<>();
            for (Student s: instance.getStudents()) {
                students.add(s);
                if (s.getGuardians().contains(user)) {
                    addIfAbsent(s.getEClass().getTeachers(), users, uniqueUsers);
                }
            }
        }
    }
}
