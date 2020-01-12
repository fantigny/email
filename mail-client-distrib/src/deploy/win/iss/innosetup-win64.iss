; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "${app.name}"
#define MyAppVersion "${version}"
#define MyAppPublisher "SpeederPan Service Ltd"
#define MyAppURL "http://speederpan.com"
#define MyAppExeName "${app.name}.exe"

#define JrePath "${ISCC.jre.drive}${jre.win64}"

[Setup]
AppId={{97A38B2F-861C-4C9B-A79A-CBBDA6157A71}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=.
OutputBaseFilename=FisherMail-win64
SetupIconFile=..\..\mail-common\src\main\resources\net\anfoya\mail\img\Mail.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
SourceDir=.

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "FisherMail.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#JrePath}\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; AppUserModelID: "FisherMail"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; AppUserModelID: "FisherMail"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

