package com.example.hookapp;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        ClassA classA = new ClassA();
        try {
            Class<?> aClass = Class.forName("com.example.hookapp.ClassA");
            Field studentField = aClass.getDeclaredField("student");
            studentField.setAccessible(true);
            Object studentObj = studentField.get(null);
            Student student = (Student) studentObj;
            System.out.println("student:"+ClassA.student.name);

            Student newStudent = new Student();
            newStudent.name = "zzz";
            studentField.set(null,newStudent);
            System.out.println("student:"+ClassA.student.name);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}