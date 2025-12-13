@echo off
echo Compiling Java project...

REM Create output directory
if not exist "out" mkdir out

REM Compile all Java files
javac -cp "mysql-connector-j-8.2.0.jar;main/src" -d out -encoding UTF-8 ^
    main/src/app/Application.java ^
    main/src/auth/AuthService.java ^
    main/src/auth/PasswordHasher.java ^
    main/src/controllers/BaseMenuController.java ^
    main/src/controllers/JuniorDeveloperMenuController.java ^
    main/src/controllers/ManagerMenuController.java ^
    main/src/controllers/SeniorDeveloperMenuController.java ^
    main/src/controllers/TesterMenuController.java ^
    main/src/dao/ContactDao.java ^
    main/src/dao/ContactDaoImplementation.java ^
    main/src/dao/UserDao.java ^
    main/src/dao/UserDaoImplementation.java ^
    main/src/db/DatabaseConnection.java ^
    main/src/exceptions/AuthenticationException.java ^
    main/src/exceptions/DatabaseException.java ^
    main/src/exceptions/InvalidInputException.java ^
    main/src/menus/JuniorDeveloperMenu.java ^
    main/src/menus/LoginMenu.java ^
    main/src/menus/ManagerMenu.java ^
    main/src/menus/SeniorDeveloperMenu.java ^
    main/src/menus/TesterMenu.java ^
    main/src/models/Contact.java ^
    main/src/models/Role.java ^
    main/src/models/User.java ^
    main/src/services/ContactService.java ^
    main/src/services/StatisticalInfoService.java ^
    main/src/services/UndoService.java ^
    main/src/services/UserService.java ^
    main/src/utils/AsciiAnimations.java ^
    main/src/utils/ConsoleColor.java ^
    main/src/utils/ConsoleUtils.java ^
    main/src/utils/InputValidator.java

if %errorlevel% equ 0 (
    echo.
    echo Compilation successful! 
    echo Run the application with: run.bat
) else (
    echo.
    echo Compilation failed! Check the errors above.
    exit /b 1
)

