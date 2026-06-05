# Interakzio Diagrama – WebHardMon

```mermaid
flowchart TD
    Login[Login]
    Dec{Redirecting}
    Main[Dashboard]
    DashEmpresa[Dashboard\nEmpresa]
    DashOrdenadores[Dashboard\nOrdenadores]
    Licencias[Licencias]
    GestLicencias[Gestionar\nLicencias]
    GestUsuarios[Gestionar\nUsuarios]
    Admin[Admin\nSUPERADMIN]
    GestAdmins[Gestionar\nAdmins]

    Login -->|Press login button| Dec
    Dec -->|Redirecting OK| Main
    Dec -->|Redirecting NOOK| Login

    Main -->|Click empresa tab| DashEmpresa
    Main -->|Click ordenadores tab| DashOrdenadores
    DashEmpresa -->|Click ordenadores tab| DashOrdenadores
    DashOrdenadores -->|Click empresa tab| DashEmpresa

    Main -->|Click licencias link| Licencias
    Licencias -->|Click nueva licencia / fill form / press ok| GestLicencias
    GestLicencias -->|Press delete button| Licencias
    GestLicencias -->|Fill form and press ok| Licencias

    Licencias -->|Click nuevo usuario / fill form / press ok| GestUsuarios
    GestUsuarios -->|Press delete button| Licencias
    GestUsuarios -->|Fill form and press ok| Licencias

    Main -->|Click admin link\n[SUPERADMIN]| Admin
    Admin -->|Click nuevo admin / fill form / press ok| GestAdmins
    GestAdmins -->|Press delete button| Admin
    GestAdmins -->|Fill form and press ok| Admin

    Main -->|Click logout| Login

    style Login fill:#00C2FF,color:#fff,stroke:none
    style Dec fill:#444,color:#fff,stroke:none
    style Main fill:#00C2FF,color:#fff,stroke:none
    style DashEmpresa fill:#00C2FF,color:#fff,stroke:none
    style DashOrdenadores fill:#00C2FF,color:#fff,stroke:none
    style Licencias fill:#00C2FF,color:#fff,stroke:none
    style GestLicencias fill:#00C2FF,color:#fff,stroke:none
    style GestUsuarios fill:#00C2FF,color:#fff,stroke:none
    style Admin fill:#00C2FF,color:#fff,stroke:none
    style GestAdmins fill:#00C2FF,color:#fff,stroke:none
```
