package com.ecopoint.model;

/**
 * People present at the start of a sampling shift: students, teachers and
 * administrative or service staff (RN-022). It is the base of the per capita
 * production and is checked against the paper form like any field record.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class ShiftPopulation extends VerifiableRecord {

    private int students;
    private int teachers;
    private int staff;

    /**
     * Creates the population of a shift.
     *
     * @param id           identifier, or null if not saved yet
     * @param students     students present
     * @param teachers     teachers present
     * @param staff        administrative and service staff present
     * @param verification who typed the population and when
     */
    public ShiftPopulation(Long id, int students, int teachers, int staff, VerificationInfo verification) {
        super(id, verification);
        this.students = students;
        this.teachers = teachers;
        this.staff = staff;
    }

    /**
     * Returns the total number of people present in the shift.
     *
     * @return students + teachers + staff
     */
    public int getTotal() {
        return students + teachers + staff;
    }

    @Override
    public String getRecordType() {
        return "Población del turno";
    }

    public int getStudents() {
        return students;
    }

    public void setStudents(int students) {
        this.students = students;
    }

    public int getTeachers() {
        return teachers;
    }

    public void setTeachers(int teachers) {
        this.teachers = teachers;
    }

    public int getStaff() {
        return staff;
    }

    public void setStaff(int staff) {
        this.staff = staff;
    }
}
