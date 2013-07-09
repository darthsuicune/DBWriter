DBWriter
========

Basic Java application that eases the task of creating the DB OpenHelper, DBProvider and contract for a database in Android.

It requires a plain text file with the following format:

DatabaseName
Table1
Field1 TYPE
Field2 TYPE NOT NULL

Table2
Field1 TYPE
Field2 TYPE NOT NULL

...

Example:

ComIsATest
Table1
Field1 TEXT
Field2 TEXT NOT NULL

Table2
Field1 TEXT
Field2 TEXT
