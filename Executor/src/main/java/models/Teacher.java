package models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teachers")
public class Teacher {
    @Id
    @Column(name = "teacher_id")
    private Long teacherId;
    private String firstname;
    private String lastname;

    @JsonManagedReference
    @OneToOne(mappedBy = "teacher", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Subject subject;

    public void setSubject(Subject s){
        this.subject = s;
        s.setId(this.teacherId);
        s.setTeacher(this);
    }

    public void removeSubject(){
        if(this.subject != null){
            this.subject.setTeacher(null);
            this.subject = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Teacher teacher = (Teacher) o;
        return teacherId.equals(teacher.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId);
    }
}