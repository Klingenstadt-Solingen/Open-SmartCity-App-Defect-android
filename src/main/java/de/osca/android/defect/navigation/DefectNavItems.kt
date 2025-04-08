package de.osca.android.defect.navigation

import androidx.navigation.navDeepLink
import de.osca.android.defect.R
import de.osca.android.defect.navigation.DefectNavItems.DefectNavItem.icon
import de.osca.android.defect.navigation.DefectNavItems.DefectNavItem.route
import de.osca.android.defect.navigation.DefectNavItems.DefectNavItem.title
import de.osca.android.essentials.domain.entity.navigation.NavigationItem

/**
 * Navigation Routes for Coworking
 */
class DefectNavItems {
    /**
     * Route for the default/main screen
     * @property title title of the route (a name to display)
     * @property route route for this navItem (name is irrelevant)
     * @property icon the icon to display
     */
    object DefectNavItem : NavigationItem(
        title = R.string.defect_title,
        route = "defect",
        icon = R.drawable.ic_main_mangel,
        deepLinks = listOf(navDeepLink { uriPattern = "solingen://defect" }),
    )
}
