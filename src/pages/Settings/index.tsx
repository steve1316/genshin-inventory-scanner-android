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

    return (
        <View style={styles.root}>
            <ScrollView>{renderScanSettings()}</ScrollView>

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
