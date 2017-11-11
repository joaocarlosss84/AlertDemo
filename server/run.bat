@echo off

IF "%1"=="" GOTO BLANK
IF "%1"=="-i" (
	ECHO Installing the System
	GOTO INSTALL
) ELSE (
	ECHO Wrong Parameter
	GOTO DONE
)


:BLANK
REM Run "run.py" with virtal environment of folders "venv"
IF EXIST venv\Scripts\python.exe (	
	venv\Scripts\python.exe run.py		
	start "Chrome" chrome "http://localhost:8888"
) ELSE (
	echo Please install virtual environment by running run.bat -i
)
GOTO DONE

:INSTALL
IF NOT EXIST "venv\Scripts\python.exe" (	
	echo No venv environment. 
	echo Creating VENV environment...
	REM	Create a virtual environment named 'venv'
	python -m venv venv
)

echo Activating VENV

REM Activate venv
@echo off
cmd /k "cd /d venv\Scripts & activate & cd /d ..\.. & python -m pip install --upgrade pip & python -m pip install -r requirements.txt & exit()"

:DONE