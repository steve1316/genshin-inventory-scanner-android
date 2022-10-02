import Ionicons from "react-native-vector-icons/Ionicons"
import React, { useContext, useEffect, useState } from "react"
import SnackBar from "rn-snackbar-component"
import { BotStateContext } from "../../context/BotStateContext"
import { ScrollView, StyleSheet, View } from "react-native"
import TitleDivider from "../../components/TitleDivider"
import CustomCheckbox from "../../components/CustomCheckbox"
import { Divider, Text } from "react-native-elements"

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

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Rendering

    const renderScanSettings = () => {
        return (
            <View>
                <TitleDivider title="Categories to Scan" subtitle="Customize which categories in the inventory to scan." hasIcon={true} iconName="bag-personal" iconColor="#000" />

                <Divider style={{ marginBottom: 10 }} />
                <Text style={{ marginBottom: 10, color: "black" }}>{`Tested at 1080p screen resolution.`}</Text>
                <Divider style={{ marginBottom: 10 }} />

                <CustomCheckbox
                    text="Scan Weapons"
                    isChecked={bsc.settings.weapons.enableScanWeapons}
                    onPress={() => bsc.setSettings({ ...bsc.settings, weapons: { ...bsc.settings.weapons, enableScanWeapons: !bsc.settings.weapons.enableScanWeapons } })}
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
