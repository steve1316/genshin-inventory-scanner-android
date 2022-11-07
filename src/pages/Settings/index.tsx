import Ionicons from "react-native-vector-icons/Ionicons"
import React, { useContext, useEffect, useState } from "react"
import SnackBar from "rn-snackbar-component"
import { BotStateContext } from "../../context/BotStateContext"
import { ScrollView, StyleSheet, View } from "react-native"
import TitleDivider from "../../components/TitleDivider"
import CustomCheckbox from "../../components/CustomCheckbox"
import { Divider, Text } from "react-native-elements"
import { TextInput } from "react-native-paper"

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        margin: 10,
    },
})

const Settings = () => {
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false)

    const bsc = useContext(BotStateContext)

    useEffect(() => {
        // Manually set this flag to false as the snackbar autohiding does not set this to false automatically.
        setSnackbarOpen(true)
        setTimeout(() => setSnackbarOpen(false), 1500)
    }, [bsc.readyStatus])

    useEffect(() => {
        if (bsc.settings.weapons.enableScanWeapons) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: false } })
        }
    }, [bsc.settings.weapons.enableScanWeapons])

    useEffect(() => {
        if (!bsc.settings.weapons.scan5StarWeapons && !bsc.settings.weapons.scan4StarWeapons && !bsc.settings.weapons.scan3StarWeapons) {
            bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, scan5StarWeapons: true } })
        }
    }, [bsc.settings.weapons])

    useEffect(() => {
        if (bsc.settings.artifacts.enableScanArtifacts) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: false } })
        }
    }, [bsc.settings.artifacts.enableScanArtifacts])

    useEffect(() => {
        if (!bsc.settings.artifacts.scan5StarArtifacts && !bsc.settings.artifacts.scan4StarArtifacts && !bsc.settings.artifacts.scan3StarArtifacts) {
            bsc.setSettings({ ...bsc.settings, artifacts: { ...bsc.settings.artifacts, scan5StarArtifacts: true } })
        }
    }, [bsc.settings.artifacts])

    useEffect(() => {
        if (bsc.settings.materials.enableScanMaterials) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: false } })
        }
    }, [bsc.settings.materials.enableScanMaterials])

    useEffect(() => {
        if (bsc.settings.materials.enableScanCharacterDevelopmentItems) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: false } })
        }
    }, [bsc.settings.materials.enableScanCharacterDevelopmentItems])

    useEffect(() => {
        if (bsc.settings.characters.enableScanCharacters) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: false } })
        }
    }, [bsc.settings.characters.enableScanCharacters])

    useEffect(() => {
        if (bsc.settings.misc.enableTestSingleSearch) {
            bsc.setSettings({
                ...bsc.settings,
                weapons: { ...bsc.settings.weapons, enableScanWeapons: false },
                artifacts: { ...bsc.settings.artifacts, enableScanArtifacts: false },
                materials: { ...bsc.settings.materials, enableScanMaterials: false, enableScanCharacterDevelopmentItems: false },
                characters: { ...bsc.settings.characters, enableScanCharacters: false },
            })
        }
    }, [bsc.settings.misc.enableTestSingleSearch])

    useEffect(() => {
        if (bsc.settings.misc.testSearchWeapon) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchArtifact: false, testSearchMaterial: false, testSearchCharacter: false } })
        }
    }, [bsc.settings.misc.testSearchWeapon])

    useEffect(() => {
        if (bsc.settings.misc.testSearchArtifact) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchWeapon: false, testSearchMaterial: false, testSearchCharacter: false } })
        }
    }, [bsc.settings.misc.testSearchArtifact])

    useEffect(() => {
        if (bsc.settings.misc.testSearchMaterial) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchWeapon: false, testSearchArtifact: false, testSearchCharacter: false } })
        }
    }, [bsc.settings.misc.testSearchMaterial])

    useEffect(() => {
        if (bsc.settings.misc.testSearchCharacter) {
            bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchWeapon: false, testSearchArtifact: false, testSearchMaterial: false } })
        }
    }, [bsc.settings.misc.testSearchCharacter])

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Rendering

    const renderScanSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Traveler Name for Character Scan"
                    subtitle={`Enter your name for the tool to correctly scan your Traveler if Character Scan is enabled.`}
                    hasIcon={true}
                    iconName="form-textbox"
                    iconColor="#000"
                />

                <TextInput
                    label="Traveler Name"
                    right={<TextInput.Icon name="close" onPress={() => bsc.setSettings({ ...bsc.settings, characters: { ...bsc.settings.characters, travelerName: "" } })} />}
                    mode="outlined"
                    value={bsc.settings.characters.travelerName}
                    onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, characters: { ...bsc.settings.characters, travelerName: value } })}
                    autoComplete={false}
                />

                <TitleDivider
                    title="Categories to Scan"
                    subtitle={`Customize which categories in the inventory to scan.\nTested at 1080p screen resolution.`}
                    hasIcon={true}
                    iconName="bag-personal"
                    iconColor="#000"
                />

                <CustomCheckbox
                    text="Enable Scan for Weapons"
                    isChecked={bsc.settings.weapons.enableScanWeapons}
                    onPress={() => bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, enableScanWeapons: !bsc.settings.weapons.enableScanWeapons } })}
                />

                {bsc.settings.weapons.enableScanWeapons ? (
                    <View>
                        <Divider style={{ marginBottom: 10, marginTop: 10 }} />
                        <Text style={{ marginBottom: 10, color: "black" }}>{`5* Weapon scanning is the default.`}</Text>

                        <View style={{ alignItems: "center" }}>
                            <CustomCheckbox
                                text="Scan 5* Weapons"
                                isChecked={bsc.settings.weapons.scan5StarWeapons}
                                onPress={() => bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, scan5StarWeapons: !bsc.settings.weapons.scan5StarWeapons } })}
                            />
                            <CustomCheckbox
                                text="Scan 4* Weapons"
                                isChecked={bsc.settings.weapons.scan4StarWeapons}
                                onPress={() => bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, scan4StarWeapons: !bsc.settings.weapons.scan4StarWeapons } })}
                            />
                            <CustomCheckbox
                                text="Scan 3* Weapons"
                                isChecked={bsc.settings.weapons.scan3StarWeapons}
                                onPress={() => bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, scan3StarWeapons: !bsc.settings.weapons.scan3StarWeapons } })}
                            />
                        </View>

                        <Divider style={{ marginBottom: 10, marginTop: 10 }} />
                    </View>
                ) : null}

                <CustomCheckbox
                    text="Enable Scan for Artifacts"
                    isChecked={bsc.settings.artifacts.enableScanArtifacts}
                    onPress={() => bsc.setSettings({ ...bsc.settings, artifacts: { ...bsc.settings.artifacts, enableScanArtifacts: !bsc.settings.artifacts.enableScanArtifacts } })}
                />

                {bsc.settings.artifacts.enableScanArtifacts ? (
                    <View>
                        <Divider style={{ marginBottom: 10, marginTop: 10 }} />
                        <Text style={{ marginBottom: 10, color: "black" }}>{`5* Artifact scanning is the default.`}</Text>

                        <View style={{ alignItems: "center" }}>
                            <CustomCheckbox
                                text="Scan 5* Artifacts"
                                isChecked={bsc.settings.artifacts.scan5StarArtifacts}
                                onPress={() => bsc.setSettings({ ...bsc.settings, artifacts: { ...bsc.settings.artifacts, scan5StarArtifacts: !bsc.settings.artifacts.scan5StarArtifacts } })}
                            />
                            <CustomCheckbox
                                text="Scan 4* Artifacts"
                                isChecked={bsc.settings.artifacts.scan4StarArtifacts}
                                onPress={() => bsc.setSettings({ ...bsc.settings, artifacts: { ...bsc.settings.artifacts, scan4StarArtifacts: !bsc.settings.artifacts.scan4StarArtifacts } })}
                            />
                            <CustomCheckbox
                                text="Scan 3* Artifacts"
                                isChecked={bsc.settings.artifacts.scan3StarArtifacts}
                                onPress={() => bsc.setSettings({ ...bsc.settings, artifacts: { ...bsc.settings.artifacts, scan3StarArtifacts: !bsc.settings.artifacts.scan3StarArtifacts } })}
                            />
                        </View>

                        <Divider style={{ marginBottom: 10, marginTop: 10 }} />
                    </View>
                ) : null}

                <CustomCheckbox
                    text="Enable Scan for Materials"
                    isChecked={bsc.settings.materials.enableScanMaterials}
                    onPress={() => bsc.setSettings({ ...bsc.settings, materials: { ...bsc.settings.materials, enableScanMaterials: !bsc.settings.materials.enableScanMaterials } })}
                />

                <CustomCheckbox
                    text="Enable Scan for Character Development Items"
                    isChecked={bsc.settings.materials.enableScanCharacterDevelopmentItems}
                    onPress={() =>
                        bsc.setSettings({ ...bsc.settings, materials: { ...bsc.settings.materials, enableScanCharacterDevelopmentItems: !bsc.settings.materials.enableScanCharacterDevelopmentItems } })
                    }
                />

                <CustomCheckbox
                    text="Enable Scan for Characters"
                    isChecked={bsc.settings.characters.enableScanCharacters}
                    onPress={() => bsc.setSettings({ ...bsc.settings, characters: { ...bsc.settings.characters, enableScanCharacters: !bsc.settings.characters.enableScanCharacters } })}
                />
            </View>
        )
    }

    const renderMiscSettings = () => {
        return (
            <View>
                <TitleDivider title="Misc Settings" subtitle="Below are miscelleneous settings mainly for debugging purposes." hasIcon={true} iconName="content-save-cog" iconColor="#000" />

                <CustomCheckbox
                    isChecked={bsc.settings.misc.debugMode}
                    onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, debugMode: !bsc.settings.misc.debugMode } })}
                    text="Enable Debug Mode"
                    subtitle="Check this to enable more detailed log messages and debugging screenshots to be saved to the /temp/ folder."
                />

                <CustomCheckbox
                    isChecked={bsc.settings.misc.enableTestSingleSearch}
                    onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, enableTestSingleSearch: !bsc.settings.misc.enableTestSingleSearch } })}
                    text="Enable Test Single Search"
                    subtitle="Check this to enable single scan for the currently selected weapon/artifact/etc on the screen. You need to enable one of the tests below in order to proceed."
                />

                {bsc.settings.misc.enableTestSingleSearch ? (
                    <View>
                        <CustomCheckbox
                            isChecked={bsc.settings.misc.testSearchWeapon}
                            onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchWeapon: !bsc.settings.misc.testSearchWeapon } })}
                            text="Test Single Weapon Scan"
                            subtitle="Check this to test scanning the currently selected weapon. Only one can be active at a time."
                        />

                        <CustomCheckbox
                            isChecked={bsc.settings.misc.testSearchArtifact}
                            onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchArtifact: !bsc.settings.misc.testSearchArtifact } })}
                            text="Test Single Artifact Scan"
                            subtitle="Check this to test scanning the currently selected artifact. Only one can be active at a time."
                        />

                        <CustomCheckbox
                            isChecked={bsc.settings.misc.testSearchMaterial}
                            onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchMaterial: !bsc.settings.misc.testSearchMaterial } })}
                            text="Test Single Material Scan"
                            subtitle="Check this to test scanning the currently selected material. Can also be used for a character development item. Only one can be active at a time."
                        />

                        <CustomCheckbox
                            isChecked={bsc.settings.misc.testSearchCharacter}
                            onPress={() => bsc.setSettings({ ...bsc.settings, misc: { ...bsc.settings.misc, testSearchCharacter: !bsc.settings.misc.testSearchCharacter } })}
                            text="Test Single Character Scan"
                            subtitle="Check this to test scanning the currently selected Character. Only one can be active at a time."
                        />
                    </View>
                ) : null}
            </View>
        )
    }

    return (
        <View style={styles.root}>
            <ScrollView>
                {renderScanSettings()}
                {renderMiscSettings()}
            </ScrollView>

            <SnackBar
                visible={snackbarOpen}
                message={bsc.readyStatus ? "Bot is ready!" : "Bot is not ready!"}
                actionHandler={() => setSnackbarOpen(false)}
                action={<Ionicons name="close" size={30} />}
                autoHidingTime={1500}
                containerStyle={{ backgroundColor: bsc.readyStatus ? "green" : "red", borderRadius: 10 }}
                native={false}
            />
        </View>
    )
}

export default Settings
