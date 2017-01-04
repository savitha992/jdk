/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8167057
 * @summary Tests --list-deps and --list-reduced-deps options
 * @modules java.logging
 *          java.xml
 *          jdk.compiler
 *          jdk.jdeps
 *          jdk.unsupported
 * @library ../lib
 * @build CompilerUtils JdepsRunner
 * @run testng ListModuleDeps
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ListModuleDeps {
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");
    private static final Path LIB_DIR = Paths.get("lib");

    private static final Path HI_CLASS =
        CLASSES_DIR.resolve("hi").resolve("Hi.class");
    private static final Path FOO_CLASS =
        CLASSES_DIR.resolve("z").resolve("Foo.class");
    private static final Path BAR_CLASS =
        CLASSES_DIR.resolve("z").resolve("Bar.class");
    private static final Path UNSAFE_CLASS =
        CLASSES_DIR.resolve("z").resolve("UseUnsafe.class");

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        // compile library
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "lib"), LIB_DIR));

        // simple program depends only on java.base
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "hi"), CLASSES_DIR));

        // compile classes in unnamed module
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "z"),
            CLASSES_DIR,
            "-cp", LIB_DIR.toString(),
            "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-exports=java.base/sun.security.util=ALL-UNNAMED",
            "--add-exports=java.xml/jdk.xml.internal=ALL-UNNAMED"
        ));
    }

    @DataProvider(name = "jdkModules")
    public Object[][] jdkModules() {
        return new Object[][]{
            {"jdk.compiler", new String[]{
                                "java.base/sun.reflect.annotation",
                                "java.compiler",
                             }
            },
        };
    }

    @Test(dataProvider = "jdkModules")
    public void testJDKModule(String moduleName, String[] expected) {
        JdepsRunner jdeps = JdepsRunner.run(
            "--list-deps", "-m", moduleName
        );
        String[] output = Arrays.stream(jdeps.output())
                                .map(s -> s.trim())
                                .toArray(String[]::new);
        assertEquals(output, expected);
    }

    @Test(dataProvider = "listdeps")
    public void testListDeps(Path classes, String[] expected) {
        JdepsRunner jdeps = JdepsRunner.run(
            "--class-path", LIB_DIR.toString(),
            "--list-deps", classes.toString()
        );
        String[] output = Arrays.stream(jdeps.output())
                                .map(s -> s.trim())
                                .toArray(String[]::new);
        assertEquals(output, expected);
    }

    @Test(dataProvider = "reduceddeps")
    public void testListReducedDeps(Path classes, String[]  expected) {
        JdepsRunner jdeps = JdepsRunner.run(
            "--class-path", LIB_DIR.toString(),
            "--list-reduced-deps", classes.toString()
        );
        String[] output = Arrays.stream(jdeps.output())
                                .map(s -> s.trim())
                                .toArray(String[]::new);
        assertEquals(output, expected);
    }


    @DataProvider(name = "listdeps")
    public Object[][] listdeps() {
        return new Object[][] {
            { CLASSES_DIR,  new String[] {
                                "java.base/jdk.internal.misc",
                                "java.base/sun.security.util",
                                "java.logging",
                                "java.sql",
                                "java.xml/jdk.xml.internal",
                                "jdk.unsupported",
                                "unnamed module: lib"
                            }
            },

            { HI_CLASS,     new String[] {
                                "java.base"
                            }
            },

            { FOO_CLASS,    new String[] {
                                "java.base",
                                "java.logging",
                                "java.sql",
                                "java.xml",
                                "unnamed module: lib"
                            }
            },

            { BAR_CLASS,    new String[] {
                                "java.base/sun.security.util",
                                "java.xml/jdk.xml.internal",
                            }
            },

            { UNSAFE_CLASS, new String[] {
                                "java.base/jdk.internal.misc",
                                "jdk.unsupported",
                            }
            },
        };
    }

    @DataProvider(name = "reduceddeps")
    public Object[][] reduceddeps() {
        Path barClass = CLASSES_DIR.resolve("z").resolve("Bar.class");

        return new Object[][] {
            { CLASSES_DIR,  new String[] {
                                "java.base/jdk.internal.misc",
                                "java.base/sun.security.util",
                                "java.sql",
                                "java.xml/jdk.xml.internal",
                                "jdk.unsupported",
                                "unnamed module: lib"
                            }
            },

            { HI_CLASS,     new String[] {
                                "java.base"
                            }
            },

            { FOO_CLASS,    new String[] {
                                "java.sql",
                                "unnamed module: lib"
                            }
            },

            { BAR_CLASS,    new String[] {
                                "java.base/sun.security.util",
                                "java.xml/jdk.xml.internal",
                            }
            },

            { UNSAFE_CLASS, new String[] {
                                "java.base/jdk.internal.misc",
                                "jdk.unsupported",
                            }
            },
        };
    }

}
