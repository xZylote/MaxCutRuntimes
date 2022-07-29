Unfortunately, the open-source library lp_solve was written in C, so we have to use something called a "Java wrapper" to call methods of the dependent libraries from Java.
This library needs to be compiled for each target platform, but luckily I included precompiled files for Windows and Linux.

The documentation states that you need to have JRE 1.3 or later installed (https://www.java.com/download/manual.jsp).
I have tested everything with JDK 15.0.1, class version 59.0 (https://www.oracle.com/java/technologies/javase/jdk15-archive-downloads.html).

To install LpSolve on

Windows:
1. Navigate to '\Library\win32' or '\Library\win64',  depending on which operating system you use.
2. Copy all files from that folder into a library directory of your choice (for example: '\Windows\').
3. If your directory of choice is not included in the PATH system variable, you have to add it.
3. Navigate to '\Wrapper\lib\' and then to 'win32' or 'win64' depending on your operating system.
4. Copy the file 'lpsolve55j.dll' to the directory you chose in step 2 (a file named 'lpsolve55.dll' should already be contained there by now).

Linux:
1. Navigate to '/Library/ux32' or '/Library/ux64',  depending on which operating system you use.
2. Copy all files from that folder into a library directory of your choice (for example: '/usr/local/lib').
3. If your directory of choice is not included in the LD_LIBRARY_PATH system variable, you have to add it.
3. Navigate to '/Wrapper/lib/' and then to 'ux32' or 'ux64' depending on your operating system.
4. Copy the file 'liblpsolve55j.so' to the directory you chose in step 2 (a file named 'liblpsolve55.so' should already be contained there by now).
5. Run 'ldconfig' to include the library in the shared library cache.

- If you get a java.lang.UnsatisfiedLinkError when trying to run exam.Test, ensure that the folder you chose in step 2 is included in the PATH/LD_LIBRARY_PATH.
- You might need to restart your terminal after making changes to the PATH/LD_LIBRARY_PATH.