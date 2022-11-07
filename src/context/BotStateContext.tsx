import React, { createContext, useState } from "react"

export interface Settings {
    weapons: {
        enableScanWeapons: boolean
        scan5StarWeapons: boolean
        scan4StarWeapons: boolean
        scan3StarWeapons: boolean
    }
    artifacts: {
        enableScanArtifacts: boolean
        scan5StarArtifacts: boolean
        scan4StarArtifacts: boolean
        scan3StarArtifacts: boolean
    }
    materials: {
        enableScanMaterials: boolean
        enableScanCharacterDevelopmentItems: boolean
    }
    characters: {
        enableScanCharacters: boolean
        travelerName: string
    }
    misc: {
        debugMode: boolean
        enableTestSingleSearch: boolean
        testSearchWeapon: boolean
        testSearchArtifact: boolean
        testSearchMaterial: boolean
        testSearchCharacter: boolean
    }
}

// Set the default settings.
export const defaultSettings: Settings = {
    weapons: {
        enableScanWeapons: false,
        scan5StarWeapons: true,
        scan4StarWeapons: false,
        scan3StarWeapons: false,
    },
    artifacts: {
        enableScanArtifacts: false,
        scan5StarArtifacts: true,
        scan4StarArtifacts: false,
        scan3StarArtifacts: false,
    },
    materials: {
        enableScanMaterials: false,
        enableScanCharacterDevelopmentItems: false,
    },
    characters: {
        enableScanCharacters: false,
        travelerName: "",
    },
    misc: {
        debugMode: false,
        enableTestSingleSearch: false,
        testSearchWeapon: false,
        testSearchArtifact: false,
        testSearchMaterial: false,
        testSearchCharacter: false,
    },
}

interface IProviderProps {
    readyStatus: boolean
    setReadyStatus: (readyStatus: boolean) => void
    isBotRunning: boolean
    setIsBotRunning: (isBotRunning: boolean) => void
    startBot: boolean
    setStartBot: (startBot: boolean) => void
    stopBot: boolean
    setStopBot: (stopBot: boolean) => void
    settings: Settings
    setSettings: (settings: Settings) => void
}

export const BotStateContext = createContext<IProviderProps>({} as IProviderProps)

// https://stackoverflow.com/a/60130448 and https://stackoverflow.com/a/60198351
export const BotStateProvider = ({ children }: any): JSX.Element => {
    const [readyStatus, setReadyStatus] = useState<boolean>(false)
    const [isBotRunning, setIsBotRunning] = useState<boolean>(false)
    const [startBot, setStartBot] = useState<boolean>(false)
    const [stopBot, setStopBot] = useState<boolean>(false)

    const [settings, setSettings] = useState<Settings>(defaultSettings)

    const providerValues: IProviderProps = {
        readyStatus,
        setReadyStatus,
        isBotRunning,
        setIsBotRunning,
        startBot,
        setStartBot,
        stopBot,
        setStopBot,
        settings,
        setSettings,
    }

    return <BotStateContext.Provider value={providerValues}>{children}</BotStateContext.Provider>
}
